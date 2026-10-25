package com.grupo140.turnos.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final UUID companyId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

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
    private NotificationService notificationService;

    private AppointmentService appointmentService;

    private TimeZone processZone;

    @BeforeEach
    void setUp() {
        // La zona del proceso queda en UTC, como en Render: el conteo tiene que usar la zona de la empresa.
        processZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        appointmentService = new AppointmentService(
                appointmentRepository,
                catalogService,
                scheduleService,
                companyService,
                userService,
                notificationService,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @AfterEach
    void restoreProcessZone() {
        TimeZone.setDefault(processZone);
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
