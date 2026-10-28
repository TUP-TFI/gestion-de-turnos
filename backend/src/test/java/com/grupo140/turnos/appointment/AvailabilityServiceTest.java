package com.grupo140.turnos.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.Service;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.schedule.DayWindow;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.settings.PlatformSettingsResponse;
import com.grupo140.turnos.settings.PlatformSettingsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
class AvailabilityServiceTest {

    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");

    // Sábado 10/10 a las 09:00 en Córdoba. El lunes 12/10 es el primer día que se puede reservar con holgura.
    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

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
    private PlatformSettingsService platformSettingsService;

    private TimeZone processZone;

    @BeforeEach
    void setUp() {
        // La zona del proceso queda en UTC, como en Render: la disponibilidad tiene que usar la de la empresa.
        processZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreProcessZone() {
        TimeZone.setDefault(processZone);
    }

    @Test
    void nineAmInCordobaIsNoonUtc() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY, window("09:00", "10:00", 30));

        assertThat(starts(NOW)).containsExactly(utc("2026-10-12T12:00:00Z"), utc("2026-10-12T12:30:00Z"));
    }

    @Test
    void thirtyMinuteServiceSeesLastSlot() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY, window("09:00", "12:00", 30));

        assertThat(starts(NOW)).last().isEqualTo(utc("2026-10-12T14:30:00Z"));
    }

    @Test
    void ninetyMinuteServiceDoesNotSeeLastSlots() {
        givenSettings(30, 90);
        givenService(90);
        givenWindows(MONDAY, window("09:00", "12:00", 30));

        // El último que entra es el de las 10:30 (termina a las 12:00): 11:00 y 11:30 ya no alcanzan.
        assertThat(starts(NOW)).last().isEqualTo(utc("2026-10-12T13:30:00Z"));
    }

    @Test
    void splitShiftUsesBothWindows() {
        givenSettings(30, 90);
        givenService(60);
        givenWindows(MONDAY, window("09:00", "10:00", 60), window("14:00", "15:00", 60));

        assertThat(starts(NOW)).containsExactly(utc("2026-10-12T12:00:00Z"), utc("2026-10-12T17:00:00Z"));
    }

    @Test
    void dayWithoutWindowsHasNoSlots() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY);

        assertThat(starts(NOW)).isEmpty();
    }

    @Test
    void slotsInsideMinimumNoticeAreDropped() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY, window("09:00", "12:00", 30));

        // Lunes 09:10 en Córdoba, con 30 minutos de anticipación: lo más temprano son las 09:40, y entre los
        // horarios de media hora el primero libre es el de las 10:00. Los de las 09:00 y 09:30 ya no sirven.
        List<Instant> result = starts(Instant.parse("2026-10-12T12:10:00Z"));

        assertThat(result).first().isEqualTo(utc("2026-10-12T13:00:00Z"));
        assertThat(result).doesNotContain(utc("2026-10-12T12:00:00Z"), utc("2026-10-12T12:30:00Z"));
    }

    @Test
    void pastSlotsOfTodayAreDropped() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY, window("09:00", "12:00", 30));

        // Lunes 11:00 en Córdoba (14:00 UTC): solo quedan horarios desde las 11:30.
        assertThat(starts(Instant.parse("2026-10-12T14:00:00Z"))).containsExactly(utc("2026-10-12T14:30:00Z"));
    }

    @Test
    void pastDateHasNoSlots() {
        givenSettings(30, 90);
        givenService(30);

        assertThat(starts(NOW, LocalDate.of(2026, 10, 9))).isEmpty();
        verify(scheduleService, never()).windowsFor(any(), any());
    }

    @Test
    void dateBeyondMaximumAdvanceHasNoSlots() {
        givenSettings(30, 90);
        givenService(30);
        LocalDate today = LocalDate.of(2026, 10, 10);
        // El último día reservable es hoy + 90: ese tiene horarios y el siguiente no.
        givenWindows(today.plusDays(90), window("09:00", "10:00", 30));

        assertThat(starts(NOW, today.plusDays(90))).isNotEmpty();
        assertThat(starts(NOW, today.plusDays(91))).isEmpty();
    }

    @Test
    void overlappingAppointmentBlocksSlotsButTouchingOneDoesNot() {
        givenSettings(30, 90);
        givenService(60);
        givenWindows(MONDAY, window("09:00", "11:00", 30));
        // Turno de 09:30 a 10:00 en Córdoba. Con un servicio de 60 minutos, los horarios de las 09:00 y las
        // 09:30 lo pisan; el de las 10:00 empieza justo cuando termina: queda libre.
        when(appointmentRepository.findAllByCompanyIdAndStatusNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                        eq(companyId), eq(AppointmentStatus.CANCELLED), any(), any()))
                .thenReturn(List.of(appointment("2026-10-12T12:30:00Z", 30)));

        assertThat(starts(NOW)).containsExactly(utc("2026-10-12T13:00:00Z"));
    }

    @Test
    void cancelledAppointmentsAreLeftOutOfTheQuery() {
        givenSettings(30, 90);
        givenService(30);
        givenWindows(MONDAY, window("09:00", "10:00", 30));

        starts(NOW);

        // El día se consulta completo, en la zona de la empresa: del lunes 00:00 al martes 00:00 en Córdoba.
        verify(appointmentRepository)
                .findAllByCompanyIdAndStatusNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                        companyId,
                        AppointmentStatus.CANCELLED,
                        utc("2026-10-13T03:00:00Z"),
                        utc("2026-10-12T03:00:00Z"));
    }

    @Test
    void resultIsSortedAndWithoutDuplicates() {
        givenSettings(30, 90);
        givenService(30);
        // Franjas desordenadas y que generan el mismo instante de las 09:00.
        givenWindows(MONDAY, window("14:00", "15:00", 60), window("09:00", "10:00", 60), window("09:00", "09:30", 30));

        assertThat(starts(NOW)).containsExactly(utc("2026-10-12T12:00:00Z"), utc("2026-10-12T17:00:00Z"));
    }

    @Test
    void inactiveServiceIsRejected() {
        when(catalogService.requireActiveService(companyId, serviceId))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "El servicio no existe"));

        assertThatThrownBy(() -> starts(NOW)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getCode()).isEqualTo("SERVICE_NOT_FOUND");
        });
        verify(scheduleService, never()).windowsFor(any(), any());
    }

    private List<Instant> starts(Instant now) {
        return starts(now, MONDAY);
    }

    private List<Instant> starts(Instant now, LocalDate date) {
        AvailabilityService availabilityService = new AvailabilityService(
                appointmentRepository,
                catalogService,
                scheduleService,
                companyService,
                platformSettingsService,
                Clock.fixed(now, ZoneOffset.UTC));
        return availabilityService.availableStarts(companyId, serviceId, date);
    }

    private void givenSettings(int minBookingNoticeMinutes, int maxBookingAdvanceDays) {
        when(companyService.zoneOf(companyId)).thenReturn(CORDOBA);
        when(platformSettingsService.get())
                .thenReturn(new PlatformSettingsResponse(minBookingNoticeMinutes, 3, 2, maxBookingAdvanceDays));
    }

    private void givenService(int durationMinutes) {
        when(catalogService.requireActiveService(companyId, serviceId))
                .thenReturn(new Service(companyId, "Corte de pelo", BigDecimal.TEN, durationMinutes));
    }

    private void givenWindows(LocalDate date, DayWindow... windows) {
        when(scheduleService.windowsFor(companyId, date)).thenReturn(List.of(windows));
    }

    private static DayWindow window(String start, String end, int intervalMinutes) {
        return new DayWindow(LocalTime.parse(start), LocalTime.parse(end), intervalMinutes);
    }

    private static Instant utc(String instant) {
        return Instant.parse(instant);
    }

    private static Appointment appointment(String startUtc, int durationMinutes) {
        Instant start = Instant.parse(startUtc);
        Appointment appointment = new Appointment();
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(durationMinutes)));
        return appointment;
    }
}
