package com.grupo140.turnos.appointment;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de los turnos.
 */
@Service
public class AppointmentService {

    // Motivo que queda guardado en los turnos cancelados por la baja de la empresa (US-04.4).
    static final String COMPANY_DEACTIVATED_REASON = "La empresa fue dada de baja";

    private final AppointmentRepository appointmentRepository;
    private final CatalogService catalogService;
    private final ScheduleService scheduleService;
    private final CompanyService companyService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final Clock clock;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            CatalogService catalogService,
            ScheduleService scheduleService,
            CompanyService companyService,
            UserService userService,
            NotificationService notificationService,
            Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.catalogService = catalogService;
        this.scheduleService = scheduleService;
        this.companyService = companyService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Cantidad de turnos {@link AppointmentStatus#PENDING} futuros de un servicio, para avisarle al admin
     * antes de desactivarlo (US-05.2).
     *
     * @throws ApiException {@code SERVICE_NOT_FOUND} (404) si el servicio no es de la empresa.
     */
    @Transactional(readOnly = true)
    public long countFutureByService(UUID companyId, UUID serviceId) {
        catalogService.requireOwnedService(companyId, serviceId);
        return appointmentRepository.countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(
                companyId, serviceId, AppointmentStatus.PENDING, Instant.now(clock));
    }

    /**
     * Cantidad de turnos {@link AppointmentStatus#PENDING} futuros que quedarían fuera de un horario semanal
     * propuesto, para avisarle al admin antes de guardarlo (US-05.4). No guarda nada: guardar el horario
     * nunca cancela ni modifica turnos.
     *
     * @throws ApiException {@code INVALID_BUSINESS_HOURS} (400) si el horario propuesto no es válido.
     */
    @Transactional(readOnly = true)
    public long countOutOfRange(UUID companyId, BusinessHoursRequest request) {
        scheduleService.validate(request);
        ZoneId zone = companyService.zoneOf(companyId);
        // TODO(US-05.6): un turno en una fecha con excepción de calendario se rige por la excepción, no por el
        // horario semanal, así que no debería contar. Ajustarlo cuando exista la lectura de ScheduleException.
        return appointmentRepository
                .findAllByCompanyIdAndStatusAndStartDateTimeAfter(
                        companyId, AppointmentStatus.PENDING, Instant.now(clock))
                .stream()
                .filter(appointment -> !fitsInHours(appointment, zone, request.hours()))
                .count();
    }

    /**
     * Indica si el turno entra completo en alguna franja del día de la semana en que cae, con el inicio y el
     * fin pasados a la hora local de la empresa. Un turno que termina en otro día local no entra.
     */
    static boolean fitsInHours(Appointment appointment, ZoneId zone, List<BusinessHoursRequest.Slot> hours) {
        ZonedDateTime start = appointment.getStartDateTime().atZone(zone);
        ZonedDateTime end = appointment.getEndDateTime().atZone(zone);
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        return hours.stream()
                .filter(slot -> slot.dayOfWeek() == start.getDayOfWeek())
                .anyMatch(slot -> !start.toLocalTime().isBefore(slot.startTime())
                        && !end.toLocalTime().isAfter(slot.endTime()));
    }

    /**
     * Cancela todos los turnos pendientes de una empresa que se desactivó (US-04.4). Quedan cancelados por la
     * empresa, con la fecha y el motivo; al reactivarla no se recuperan (US-04.5).
     *
     * @return cuántos turnos se cancelaron.
     */
    @Transactional
    public int cancelPendingByCompany(UUID companyId) {
        Instant now = Instant.now(clock);
        List<Appointment> pending =
                appointmentRepository.findByCompanyIdAndStatus(companyId, AppointmentStatus.PENDING);
        for (Appointment appointment : pending) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelledBy(CancelledBy.COMPANY);
            appointment.setCancelledAt(now);
            appointment.setCancellationReason(COMPANY_DEACTIVATED_REASON);
            String phone = clientPhone(appointment);
            if (phone != null) {
                notificationService.sendAppointmentCancelled(
                        phone, appointment.getStartDateTime(), COMPANY_DEACTIVATED_REASON);
            }
        }
        return pending.size();
    }

    // El cliente registrado tiene su teléfono en su cuenta; el turno cargado a mano por el admin lo guarda en el
    // propio turno. Si no hay ninguno de los dos, no hay a quién avisar.
    private String clientPhone(Appointment appointment) {
        if (appointment.getClientId() != null) {
            return userService
                    .findPhone(appointment.getClientId(), appointment.getCompanyId())
                    .orElse(null);
        }
        return appointment.getManualClientPhone();
    }

    /** Total de turnos de toda la plataforma, en cualquier estado, para el dashboard del superadmin (US-04.6). */
    @Transactional(readOnly = true)
    public long countAll() {
        return appointmentRepository.count();
    }
}
