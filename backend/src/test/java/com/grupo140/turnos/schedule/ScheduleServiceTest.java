package com.grupo140.turnos.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.company.CompanyService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code windowsFor} (US-05.5): qué franjas rigen en una fecha, con la excepción de calendario por encima del
 * horario semanal.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    // Lunes 12/10 y martes 13/10 de 2026.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 13);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 14);

    private final UUID companyId = UUID.randomUUID();

    @Mock
    private BusinessHoursRepository businessHoursRepository;

    @Mock
    private ScheduleExceptionRepository scheduleExceptionRepository;

    @Mock
    private CompanyService companyService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                businessHoursRepository,
                scheduleExceptionRepository,
                companyService,
                Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC));
        // Lenient: los tests con excepción no llegan a leer el horario semanal.
        lenient()
                .when(businessHoursRepository.findAllByCompanyId(companyId))
                .thenReturn(List.of(
                        hours(DayOfWeek.MONDAY, "14:00", "18:00", 60),
                        hours(DayOfWeek.MONDAY, "09:00", "13:00", 30),
                        hours(DayOfWeek.TUESDAY, "10:00", "12:00", 30)));
    }

    @Test
    void withoutExceptionUsesWeeklyHoursOfThatWeekdaySortedByStart() {
        givenNoException(MONDAY);

        assertThat(scheduleService.windowsFor(companyId, MONDAY))
                .containsExactly(window("09:00", "13:00", 30), window("14:00", "18:00", 60));
    }

    @Test
    void weekdayWithoutHoursHasNoWindows() {
        givenNoException(WEDNESDAY);

        assertThat(scheduleService.windowsFor(companyId, WEDNESDAY)).isEmpty();
    }

    @Test
    void closedExceptionReplacesTheWholeDay() {
        // El lunes tiene franjas semanales, pero la excepción cerrada manda.
        when(scheduleExceptionRepository.findByCompanyIdAndExceptionDate(companyId, MONDAY))
                .thenReturn(Optional.of(new ScheduleException(companyId, MONDAY)));

        assertThat(scheduleService.windowsFor(companyId, MONDAY)).isEmpty();
    }

    @Test
    void specialHoursExceptionReplacesTheWeeklyWindows() {
        when(scheduleExceptionRepository.findByCompanyIdAndExceptionDate(companyId, MONDAY))
                .thenReturn(Optional.of(new ScheduleException(
                        companyId, MONDAY, LocalTime.parse("10:00"), LocalTime.parse("12:00"), 20)));

        assertThat(scheduleService.windowsFor(companyId, MONDAY)).containsExactly(window("10:00", "12:00", 20));
    }

    @Test
    void exceptionOfAnotherDateDoesNotAffectThisOne() {
        givenNoException(TUESDAY);

        assertThat(scheduleService.windowsFor(companyId, TUESDAY)).containsExactly(window("10:00", "12:00", 30));
    }

    private void givenNoException(LocalDate date) {
        when(scheduleExceptionRepository.findByCompanyIdAndExceptionDate(companyId, date))
                .thenReturn(Optional.empty());
    }

    private BusinessHours hours(DayOfWeek day, String start, String end, int intervalMinutes) {
        return new BusinessHours(companyId, day, LocalTime.parse(start), LocalTime.parse(end), intervalMinutes);
    }

    private static DayWindow window(String start, String end, int intervalMinutes) {
        return new DayWindow(LocalTime.parse(start), LocalTime.parse(end), intervalMinutes);
    }
}
