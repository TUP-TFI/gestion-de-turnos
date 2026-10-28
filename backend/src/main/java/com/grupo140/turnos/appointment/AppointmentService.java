package com.grupo140.turnos.appointment;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.ServiceResponse;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.notification.NotificationService;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleExceptionRequest;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
     * nunca cancela ni modifica turnos. Los turnos en fechas con excepción de calendario no cuentan: esas
     * fechas se rigen por la excepción, no por el horario semanal (US-05.6).
     *
     * @throws ApiException {@code INVALID_BUSINESS_HOURS} (400) si el horario propuesto no es válido.
     */
    @Transactional(readOnly = true)
    public long countOutOfRange(UUID companyId, BusinessHoursRequest request) {
        scheduleService.validate(request);
        ZoneId zone = companyService.zoneOf(companyId);
        Set<LocalDate> exceptionDates = scheduleService.upcomingExceptionDates(companyId);
        return appointmentRepository
                .findAllByCompanyIdAndStatusAndStartDateTimeAfter(
                        companyId, AppointmentStatus.PENDING, Instant.now(clock))
                .stream()
                .filter(appointment -> !exceptionDates.contains(localDate(appointment, zone)))
                .filter(appointment -> !fitsInHours(appointment, zone, request.hours()))
                .count();
    }

    /**
     * Cantidad de turnos {@link AppointmentStatus#PENDING} futuros de la fecha de una excepción propuesta que
     * quedarían fuera de ella, para avisarle al admin antes de guardarla (US-05.6). Si el día queda cerrado,
     * cuentan todos los turnos de esa fecha. No guarda nada: guardar la excepción nunca cancela turnos.
     *
     * @throws ApiException {@code INVALID_SCHEDULE_EXCEPTION} (400) si la excepción propuesta no es válida.
     */
    @Transactional(readOnly = true)
    public long countAffectedByException(UUID companyId, ScheduleExceptionRequest request) {
        scheduleService.validateException(companyId, request);
        ZoneId zone = companyService.zoneOf(companyId);
        // El horario especial se arma como una franja de ese día de la semana para reusar fitsInHours.
        List<BusinessHoursRequest.Slot> hours = request.isClosed()
                ? List.of()
                : List.of(new BusinessHoursRequest.Slot(
                        request.exceptionDate().getDayOfWeek(),
                        request.startTime(),
                        request.endTime(),
                        request.intervalMinutes()));
        return appointmentRepository
                .findAllByCompanyIdAndStatusAndStartDateTimeAfter(
                        companyId, AppointmentStatus.PENDING, Instant.now(clock))
                .stream()
                .filter(appointment -> localDate(appointment, zone).equals(request.exceptionDate()))
                .filter(appointment -> !fitsInHours(appointment, zone, hours))
                .count();
    }

    // Fecha en que empieza el turno, en la hora local de la empresa.
    private static LocalDate localDate(Appointment appointment, ZoneId zone) {
        return appointment.getStartDateTime().atZone(zone).toLocalDate();
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
     * Los turnos del cliente en su empresa, del más reciente al más antiguo, con el nombre del servicio (US-07.6).
     * Si {@code status} es nulo, trae todos. Los nombres salen de un solo listado de la empresa, sin una consulta
     * por turno; incluye los servicios inactivos, que siguen figurando en turnos ya reservados.
     *
     * @param companyId empresa de la sesión.
     * @param clientId cliente de la sesión: nunca uno que llegue en la request.
     */
    @Transactional(readOnly = true)
    public List<ClientAppointmentResponse> listForClient(UUID companyId, UUID clientId, AppointmentStatus status) {
        List<Appointment> appointments;
        if (status == null) {
            appointments =
                    appointmentRepository.findAllByCompanyIdAndClientIdOrderByStartDateTimeDesc(companyId, clientId);
        } else {
            appointments = appointmentRepository.findAllByCompanyIdAndClientIdAndStatusOrderByStartDateTimeDesc(
                    companyId, clientId, status);
        }
        Map<UUID, String> serviceNames = catalogService.list(companyId).stream()
                .collect(Collectors.toMap(ServiceResponse::id, ServiceResponse::name));
        List<ClientAppointmentResponse> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            result.add(ClientAppointmentResponse.from(appointment, serviceNames.get(appointment.getServiceId())));
        }
        return result;
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
