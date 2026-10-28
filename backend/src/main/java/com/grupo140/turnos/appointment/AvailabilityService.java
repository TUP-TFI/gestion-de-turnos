package com.grupo140.turnos.appointment;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.schedule.DayWindow;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.settings.PlatformSettingsResponse;
import com.grupo140.turnos.settings.PlatformSettingsService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cálculo de disponibilidad (US-05.5): los horarios de inicio que un cliente puede reservar para un servicio en
 * una fecha. Vive en {@code appointment} porque necesita los turnos ya ocupados; {@code schedule} solo aporta
 * las franjas candidatas (ver el README, sección "Regla de dependencias entre features").
 *
 * <p>No tiene endpoint propio: lo expone la página pública (US-06.2) y la reserva lo usa para validar (US-07.3).
 */
@Service
public class AvailabilityService {

    private final AppointmentRepository appointmentRepository;
    private final CatalogService catalogService;
    private final ScheduleService scheduleService;
    private final CompanyService companyService;
    private final PlatformSettingsService platformSettingsService;
    private final Clock clock;

    public AvailabilityService(
            AppointmentRepository appointmentRepository,
            CatalogService catalogService,
            ScheduleService scheduleService,
            CompanyService companyService,
            PlatformSettingsService platformSettingsService,
            Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.catalogService = catalogService;
        this.scheduleService = scheduleService;
        this.companyService = companyService;
        this.platformSettingsService = platformSettingsService;
        this.clock = clock;
    }

    /**
     * Horarios de inicio disponibles para el servicio en esa fecha, ordenados y sin repetir. La fecha es la
     * local de la empresa. Devuelve una lista vacía si la fecha ya pasó, si cae más allá de la anticipación
     * máxima, si la empresa no abre ese día o si no queda ningún horario libre.
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404) si el servicio no existe, es de otra empresa o está
     *     inactivo.
     */
    @Transactional(readOnly = true)
    public List<Instant> availableStarts(UUID companyId, UUID serviceId, LocalDate date) {
        int durationMinutes =
                catalogService.requireActiveService(companyId, serviceId).getDurationMinutes();
        ZoneId zone = companyService.zoneOf(companyId);
        PlatformSettingsResponse settings = platformSettingsService.get();

        // "Hoy" se calcula en la zona de la empresa, no en la del servidor.
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (date.isBefore(today) || date.isAfter(today.plusDays(settings.maxBookingAdvanceDays()))) {
            return List.of();
        }

        List<DayWindow> windows = scheduleService.windowsFor(companyId, date);
        if (windows.isEmpty()) {
            return List.of();
        }

        Instant earliestStart = Instant.now(clock).plus(Duration.ofMinutes(settings.minBookingNoticeMinutes()));
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Appointment> occupied =
                appointmentRepository.findAllByCompanyIdAndStatusNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                        companyId, AppointmentStatus.CANCELLED, dayEnd, dayStart);

        // Un TreeSet deja el resultado ordenado y sin repetir: dos franjas podrían generar el mismo instante.
        SortedSet<Instant> available = new TreeSet<>();
        for (DayWindow window : windows) {
            long windowMinutes = Duration.between(window.start(), window.end()).toMinutes();
            for (long offset = 0; offset + durationMinutes <= windowMinutes; offset += window.intervalMinutes()) {
                LocalTime time = window.start().plusMinutes(offset);
                Instant start = LocalDateTime.of(date, time).atZone(zone).toInstant();
                Instant end = start.plus(Duration.ofMinutes(durationMinutes));
                if (!start.isBefore(earliestStart) && isFree(occupied, start, end)) {
                    available.add(start);
                }
            }
        }
        return List.copyOf(available);
    }

    /**
     * Horarios disponibles para la página pública (US-06.2), que llega por el slug y sin sesión. A diferencia de
     * la página (US-06.1), una empresa desactivada da error: no se le ofrecen horarios.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404), {@code COMPANY_INACTIVE} (403) o
     *     {@code SERVICE_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse publicAvailability(String slug, UUID serviceId, LocalDate date) {
        UUID companyId = companyService.requireActiveIdBySlug(slug);
        return new AvailabilityResponse(date, availableStarts(companyId, serviceId, date));
    }

    private static boolean isFree(List<Appointment> occupied, Instant start, Instant end) {
        for (Appointment appointment : occupied) {
            if (appointment.getStartDateTime().isBefore(end)
                    && appointment.getEndDateTime().isAfter(start)) {
                return false;
            }
        }
        return true;
    }
}
