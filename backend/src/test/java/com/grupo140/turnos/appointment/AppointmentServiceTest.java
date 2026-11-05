package com.grupo140.turnos.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.Service;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.notification.NotificationEventType;
import com.grupo140.turnos.notification.NotificationRequestedEvent;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.settings.PlatformSettingsResponse;
import com.grupo140.turnos.settings.PlatformSettingsService;
import com.grupo140.turnos.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final UUID companyId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private CompanyService companyService;

    @Mock
    private UserService userService;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private PlatformSettingsService platformSettingsService;

    @Mock
    private ApplicationEventPublisher events;

    private AppointmentService appointmentService;

    private TimeZone processZone;

    @BeforeEach
    void setUp() {
        // La zona del proceso queda en UTC, como en Render: el conteo tiene que usar la zona de la empresa.
        processZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        appointmentService = new AppointmentService(
                appointmentRepository,
                availabilityService,
                catalogService,
                scheduleService,
                companyService,
                userService,
                platformSettingsService,
                events,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @AfterEach
    void restoreProcessZone() {
        TimeZone.setDefault(processZone);
    }

    @Test
    void bookRejectsStartBeforeMinimumNotice() {
        givenBookableService();

        assertBookingRejected(now.plus(Duration.ofMinutes(29)), "BOOKING_TOO_SOON");
        assertBookingRejected(now.minus(Duration.ofHours(1)), "BOOKING_TOO_SOON");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void bookAcceptsStartExactlyAtMinimumNotice() {
        givenBookableService();
        Instant start = now.plus(Duration.ofMinutes(30));
        givenAvailable(start);
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        ClientAppointmentResponse response =
                appointmentService.book(companyId, clientId, new BookAppointmentRequest(serviceId, start));

        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void bookRejectsDateAfterMaximumAdvance() {
        givenBookableService();

        // 10/10 + 90 días = 8/1 en Córdoba; el 9/1 a las 10:00 ya queda afuera.
        assertBookingRejected(Instant.parse("2027-01-09T13:00:00Z"), "BOOKING_TOO_FAR");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void bookUsesCompanyDateForMaximumAdvance() {
        givenBookableService();
        // 8/1 a las 23:30 en Córdoba es 9/1 en UTC: sigue dentro de la ventana y pasa a la disponibilidad.
        Instant start = Instant.parse("2027-01-09T02:30:00Z");
        when(availabilityService.availableStarts(companyId, serviceId, LocalDate.parse("2027-01-08")))
                .thenReturn(List.of());

        assertBookingRejected(start, "SLOT_UNAVAILABLE");
    }

    @Test
    void bookRejectsStartNotAvailable() {
        givenBookableService();
        Instant start = now.plus(Duration.ofDays(1));
        when(availabilityService.availableStarts(companyId, serviceId, localDate(start)))
                .thenReturn(List.of(start.plus(Duration.ofMinutes(30))));

        assertBookingRejected(start, "SLOT_UNAVAILABLE");
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void bookCopiesServiceDataAndRequestsNotification() {
        givenBookableService();
        Instant start = now.plus(Duration.ofDays(1));
        givenAvailable(start);
        when(appointmentRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        ClientAppointmentResponse response =
                appointmentService.book(companyId, clientId, new BookAppointmentRequest(serviceId, start));

        verify(companyService).lockActive(companyId);
        assertThat(response.priceSnapshot()).isEqualByComparingTo("8000");
        assertThat(response.durationMinutesSnapshot()).isEqualTo(45);
        assertThat(response.endDateTime()).isEqualTo(start.plus(Duration.ofMinutes(45)));
        ArgumentCaptor<NotificationRequestedEvent> event = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(NotificationEventType.BOOKING_CONFIRMED);
        assertThat(event.getValue().recipientPhone()).isEqualTo("+5493510000001");
        assertThat(event.getValue().userId()).isEqualTo(clientId);
        assertThat(event.getValue().message()).contains("Corte").contains("11/10/2026 09:00");
    }

    private void givenBookableService() {
        when(catalogService.requireActiveService(companyId, serviceId))
                .thenReturn(new Service(companyId, "Corte", new BigDecimal("8000"), 45));
        when(companyService.zoneOf(companyId)).thenReturn(CORDOBA);
        when(platformSettingsService.get()).thenReturn(new PlatformSettingsResponse(30, 3, 2, 90));
    }

    private void givenAvailable(Instant start) {
        when(availabilityService.availableStarts(companyId, serviceId, localDate(start)))
                .thenReturn(List.of(start));
        when(userService.findPhone(clientId, companyId)).thenReturn(Optional.of("+5493510000001"));
    }

    private LocalDate localDate(Instant start) {
        return start.atZone(CORDOBA).toLocalDate();
    }

    private void assertBookingRejected(Instant start, String code) {
        BookAppointmentRequest request = new BookAppointmentRequest(serviceId, start);
        assertThatThrownBy(() -> appointmentService.book(companyId, clientId, request))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }

    @Test
    void countsPendingAppointmentsAfterNow() {
        when(appointmentRepository.countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(
                        companyId, serviceId, AppointmentStatus.PENDING, now))
                .thenReturn(3L);

        assertThat(appointmentService.countFutureByService(companyId, serviceId))
                .isEqualTo(3L);
    }

    @Test
    void serviceOfAnotherCompanyIsNotCounted() {
        when(catalogService.requireOwnedService(companyId, serviceId))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "El servicio no existe"));

        assertThatThrownBy(() -> appointmentService.countFutureByService(companyId, serviceId))
                .isInstanceOf(ApiException.class);
        verify(appointmentRepository, never())
                .countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(any(), any(), any(), any());
    }

    @Test
    void appointmentInsideNewHoursIsNotCounted() {
        // Lunes 12/10 a las 10:00 en Córdoba (13:00 UTC), con franja de 09:00 a 13:00.
        givenFutureAppointments(appointment("2026-10-12T13:00:00Z", 30));

        assertThat(countOutOfRange(slot(DayOfWeek.MONDAY, "09:00", "13:00"))).isZero();
    }

    @Test
    void appointmentOutsideNewHoursIsCounted() {
        givenFutureAppointments(appointment("2026-10-12T13:00:00Z", 30));

        assertThat(countOutOfRange(slot(DayOfWeek.MONDAY, "14:00", "18:00"))).isEqualTo(1);
    }

    @Test
    void appointmentEndingAfterTheSlotIsCounted() {
        // De 12:30 a 13:30 en Córdoba: empieza dentro de la franja pero termina después.
        givenFutureAppointments(appointment("2026-10-12T15:30:00Z", 60));

        assertThat(countOutOfRange(slot(DayOfWeek.MONDAY, "09:00", "13:00"))).isEqualTo(1);
    }

    @Test
    void appointmentOnAClosedDayIsCounted() {
        givenFutureAppointments(appointment("2026-10-12T13:00:00Z", 30));

        assertThat(countOutOfRange(slot(DayOfWeek.TUESDAY, "09:00", "13:00"))).isEqualTo(1);
    }

    @Test
    void invalidHoursAreRejectedBeforeCounting() {
        BusinessHoursRequest request = new BusinessHoursRequest(List.of(slot(DayOfWeek.MONDAY, "09:00", "13:00")));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BUSINESS_HOURS", "Inválido"))
                .when(scheduleService)
                .validate(request);

        assertThatThrownBy(() -> appointmentService.countOutOfRange(companyId, request))
                .isInstanceOf(ApiException.class);
        verify(appointmentRepository, never()).findAllByCompanyIdAndStatusAndStartDateTimeAfter(any(), any(), any());
    }

    private long countOutOfRange(BusinessHoursRequest.Slot... slots) {
        return appointmentService.countOutOfRange(companyId, new BusinessHoursRequest(List.of(slots)));
    }

    // El repositorio ya filtra los turnos pendientes futuros: el mock devuelve solo esos.
    private void givenFutureAppointments(Appointment... appointments) {
        when(companyService.zoneOf(companyId)).thenReturn(ZoneId.of("America/Argentina/Cordoba"));
        when(appointmentRepository.findAllByCompanyIdAndStatusAndStartDateTimeAfter(
                        companyId, AppointmentStatus.PENDING, now))
                .thenReturn(List.of(appointments));
    }

    @Test
    void clientCancelsExactlyAtDeadlineWithoutNotification() {
        Appointment appointment = givenClientAppointment(now.plus(Duration.ofHours(3)));
        when(catalogService.requireOwnedService(companyId, serviceId))
                .thenReturn(new Service(companyId, "Corte", new BigDecimal("8000"), 45));

        ClientAppointmentResponse response = appointmentService.cancelByClient(companyId, clientId, appointmentId);

        verify(companyService).lockActive(companyId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.canCancel()).isFalse();
        assertThat(appointment.getCancelledBy()).isEqualTo(CancelledBy.CLIENT);
        assertThat(appointment.getCancelledAt()).isEqualTo(now);
        verifyNoInteractions(events);
    }

    @Test
    void clientCannotCancelOneSecondAfterDeadline() {
        Appointment appointment =
                givenClientAppointment(now.plus(Duration.ofHours(3)).minusSeconds(1));

        assertThatThrownBy(() -> appointmentService.cancelByClient(companyId, clientId, appointmentId))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("CANCELLATION_DEADLINE_PASSED"));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    private Appointment givenClientAppointment(Instant start) {
        Appointment appointment = appointment(start.toString(), 45);
        appointment.setServiceId(serviceId);
        appointment.setDurationMinutesSnapshot(45);
        appointment.setStatus(AppointmentStatus.PENDING);
        when(appointmentRepository.findByIdAndCompanyIdAndClientId(appointmentId, companyId, clientId))
                .thenReturn(Optional.of(appointment));
        when(platformSettingsService.get()).thenReturn(new PlatformSettingsResponse(30, 3, 2, 90));
        return appointment;
    }

    private static Appointment appointment(String startUtc, int durationMinutes) {
        Instant start = Instant.parse(startUtc);
        Appointment appointment = new Appointment();
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(durationMinutes)));
        return appointment;
    }

    private static BusinessHoursRequest.Slot slot(DayOfWeek day, String start, String end) {
        return new BusinessHoursRequest.Slot(day, LocalTime.parse(start), LocalTime.parse(end), 30);
    }
}
