package com.grupo140.turnos.appointment;

import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.ServiceResponse;
import com.grupo140.turnos.common.PageResponse;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleExceptionRequest;
import com.grupo140.turnos.schedule.ScheduleService;
import com.grupo140.turnos.settings.PlatformSettingsResponse;
import com.grupo140.turnos.settings.PlatformSettingsService;
import com.grupo140.turnos.user.ClientResponse;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
    private final AvailabilityService availabilityService;
    private final CatalogService catalogService;
    private final ScheduleService scheduleService;
    private final CompanyService companyService;
    private final UserService userService;
    private final PlatformSettingsService platformSettingsService;
    private final Clock clock;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            CatalogService catalogService,
            ScheduleService scheduleService,
            CompanyService companyService,
            UserService userService,
            PlatformSettingsService platformSettingsService,
            Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.catalogService = catalogService;
        this.scheduleService = scheduleService;
        this.companyService = companyService;
        this.userService = userService;
        this.platformSettingsService = platformSettingsService;
        this.clock = clock;
    }

    /**
     * Reserva un turno a nombre del cliente (US-07.3). El lock de la empresa serializa sus reservas hasta el commit,
     * así que dos clientes no pueden leer el mismo horario como libre; la constraint
     * {@code excl_appointment_overlap} queda como segunda barrera. No publica un aviso: el admin avisa desde su panel
     * (ver {@code docs/contratos-api.md}, Paso 10).
     *
     * @param companyId empresa de la sesión.
     * @param clientId cliente de la sesión: nunca uno que llegue en la request.
     * @throws ApiException {@code COMPANY_INACTIVE} (403), {@code SERVICE_NOT_FOUND} (404),
     *     {@code BOOKING_TOO_SOON} (400), {@code BOOKING_TOO_FAR} (400) o {@code SLOT_UNAVAILABLE} (409).
     */
    @Transactional
    public ClientAppointmentResponse book(UUID companyId, UUID clientId, BookAppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setClientId(clientId);
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        com.grupo140.turnos.catalog.Service service =
                reserve(companyId, request.serviceId(), request.startDateTime(), appointment);
        return toResponse(appointment, service.getName(), platformSettingsService.get(), Instant.now(clock));
    }

    /**
     * Carga un turno a pedido del admin (US-09.5 y US-09.6), con las mismas validaciones y el mismo lock que la reserva
     * online. Va a nombre de un cliente con cuenta o de alguien sin cuenta, con nombre y teléfono. Un turno con cuenta
     * le aparece al cliente en "Mis turnos" como cualquier otro. No publica un aviso: el admin avisa desde su panel.
     *
     * @param companyId empresa de la sesión.
     * @throws ApiException {@code INVALID_CLIENT_IDENTITY} (400), {@code CLIENT_NOT_FOUND} (404) y los de
     *     {@link #book}.
     */
    @Transactional
    public AdminAppointmentResponse bookManual(UUID companyId, ManualAppointmentRequest request) {
        String name = blankToNull(request.manualClientName());
        String phone = blankToNull(request.manualClientPhone());
        boolean hasAccount = request.clientId() != null;
        // Exactamente una identidad, igual que el CHECK chk_appointment_client_identity: o la cuenta, o nombre y
        // teléfono.
        boolean validIdentity = hasAccount ? name == null && phone == null : name != null && phone != null;
        if (!validIdentity) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CLIENT_IDENTITY",
                    "El turno lleva un cliente con cuenta, o el nombre y el teléfono de alguien sin cuenta");
        }
        if (hasAccount && !userService.isClient(companyId, request.clientId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "El cliente no existe");
        }

        Appointment appointment = new Appointment();
        appointment.setClientId(request.clientId());
        appointment.setManualClientName(name);
        appointment.setManualClientPhone(phone);
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        reserve(companyId, request.serviceId(), request.startDateTime(), appointment);
        return toAdminResponses(companyId, List.of(appointment)).getFirst();
    }

    // Lo común a la reserva online y a la carga manual: quien llama pone el cliente y el origen, y acá se valida el
    // horario, se copian precio y duración y se guarda ese mismo turno. Devuelve el servicio ya leído, para no volver a
    // buscarlo al armar la respuesta.
    private com.grupo140.turnos.catalog.Service reserve(
            UUID companyId, UUID serviceId, Instant start, Appointment appointment) {
        companyService.lockActive(companyId);
        com.grupo140.turnos.catalog.Service service = catalogService.requireActiveService(companyId, serviceId);
        ZoneId zone = companyService.zoneOf(companyId);
        LocalDate date = start.atZone(zone).toLocalDate();
        requireWithinBookingWindow(start, date, zone);
        if (!availabilityService.availableStarts(companyId, serviceId, date).contains(start)) {
            throw slotUnavailable();
        }

        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
        appointment.setPriceSnapshot(service.getPrice());
        appointment.setDurationMinutesSnapshot(service.getDurationMinutes());
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(service.getDurationMinutes())));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointmentRepository.saveAndFlush(appointment);
        return service;
    }

    private static ApiException slotUnavailable() {
        return new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "El horario ya no está disponible");
    }

    // Anticipación mínima y máxima (US-07.4). Va separada de la disponibilidad, que solo diría "no hay horario",
    // para dar un mensaje claro. "Hoy" se calcula en la zona de la empresa, igual que en AvailabilityService.
    private void requireWithinBookingWindow(Instant start, LocalDate date, ZoneId zone) {
        PlatformSettingsResponse settings = platformSettingsService.get();
        Instant earliestStart = Instant.now(clock).plus(Duration.ofMinutes(settings.minBookingNoticeMinutes()));
        if (start.isBefore(earliestStart)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "BOOKING_TOO_SOON",
                    "El turno se tiene que reservar con al menos %d minutos de anticipación"
                            .formatted(settings.minBookingNoticeMinutes()));
        }
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (date.isAfter(today.plusDays(settings.maxBookingAdvanceDays()))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "BOOKING_TOO_FAR",
                    "El turno se puede reservar con hasta %d días de anticipación"
                            .formatted(settings.maxBookingAdvanceDays()));
        }
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
        PlatformSettingsResponse settings = platformSettingsService.get();
        Instant now = Instant.now(clock);
        List<ClientAppointmentResponse> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            result.add(toResponse(appointment, serviceNames.get(appointment.getServiceId()), settings, now));
        }
        return result;
    }

    /**
     * Cancela los turnos pendientes futuros de una empresa que se desactivó (US-04.4, US-11.2). Quedan cancelados por
     * la empresa, con la fecha y el motivo; al reactivarla no se recuperan (US-04.5). No publica avisos: el superadmin
     * avisa a cada cliente desde {@link #listCancellationNotices}.
     *
     * <p>Un turno pendiente que ya empezó no se cancela: se atendió, y lo pasa a {@code COMPLETED} el job de cierre
     * (US-11.1). Entre dos corridas del job siempre puede quedar uno así.
     *
     * @return cuántos turnos se cancelaron.
     */
    @Transactional
    public int cancelPendingByCompany(UUID companyId) {
        Instant now = Instant.now(clock);
        List<Appointment> pending = appointmentRepository.findAllByCompanyIdAndStatusAndStartDateTimeAfter(
                companyId, AppointmentStatus.PENDING, now);
        for (Appointment appointment : pending) {
            markCancelled(appointment, CancelledBy.COMPANY, COMPANY_DEACTIVATED_REASON, now);
        }
        return pending.size();
    }

    /**
     * Agenda del admin (US-09.2): todos los turnos de la empresa, del más nuevo al más viejo. Los pendientes van al
     * revés, del más cercano al más lejano, porque son los que hay que atender. Cada filtro es opcional;
     * {@code rescheduled} separa los turnos con {@code rescheduleCount > 0} de los que nunca se reprogramaron.
     *
     * @param companyId empresa de la sesión.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAppointmentResponse> listForCompany(
            UUID companyId,
            AppointmentStatus status,
            AppointmentOrigin origin,
            Boolean rescheduled,
            int page,
            int size) {
        Sort.Direction direction = status == AppointmentStatus.PENDING ? Sort.Direction.ASC : Sort.Direction.DESC;
        // El id desempata, para que dos turnos a la misma hora no salten de una página a otra.
        Sort sort = Sort.by(direction, "startDateTime").and(Sort.by("id"));
        Page<Appointment> appointments = appointmentRepository.searchForCompany(
                companyId, status, origin, rescheduled, PageRequest.of(page, size, sort));
        return new PageResponse<>(
                toAdminResponses(companyId, appointments.getContent()),
                appointments.getNumber(),
                appointments.getSize(),
                appointments.getTotalElements(),
                appointments.getTotalPages());
    }

    /**
     * Resumen del dashboard del admin (US-09.1): los turnos de hoy y los próximos 5, sin cancelados. "Hoy" es la
     * fecha local de la empresa.
     *
     * @param companyId empresa de la sesión.
     */
    @Transactional(readOnly = true)
    public AppointmentSummaryResponse summary(UUID companyId) {
        ZoneId zone = companyService.zoneOf(companyId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant();
        List<Appointment> todays = appointmentRepository.findNotCancelledBetween(companyId, todayStart, tomorrowStart);
        List<Appointment> upcoming =
                appointmentRepository
                        .findFirst5ByCompanyIdAndStatusNotAndStartDateTimeGreaterThanEqualOrderByStartDateTimeAscIdAsc(
                                companyId, AppointmentStatus.CANCELLED, tomorrowStart);
        return new AppointmentSummaryResponse(
                toAdminResponses(companyId, todays), toAdminResponses(companyId, upcoming));
    }

    // Arma los turnos del admin con el nombre del servicio y los datos del cliente. Servicios y clientes se leen en una
    // consulta cada uno, no una por turno; los servicios incluyen los inactivos.
    private List<AdminAppointmentResponse> toAdminResponses(UUID companyId, List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> serviceNames = catalogService.list(companyId).stream()
                .collect(Collectors.toMap(ServiceResponse::id, ServiceResponse::name));
        Set<UUID> clientIds = appointments.stream()
                .map(Appointment::getClientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ClientResponse> clients = userService.findClients(companyId, clientIds).stream()
                .collect(Collectors.toMap(ClientResponse::id, Function.identity()));

        List<AdminAppointmentResponse> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            ClientResponse account = clients.get(appointment.getClientId());
            AdminAppointmentResponse.Client client = account != null
                    ? new AdminAppointmentResponse.Client(
                            account.id(), account.firstName() + " " + account.lastName(), account.phone())
                    : new AdminAppointmentResponse.Client(
                            null, appointment.getManualClientName(), appointment.getManualClientPhone());
            result.add(
                    AdminAppointmentResponse.from(appointment, serviceNames.get(appointment.getServiceId()), client));
        }
        return result;
    }

    /**
     * Turnos futuros que se cancelaron al desactivar la empresa, para que el superadmin avise a cada cliente por
     * WhatsApp (US-04.4). Ordenados por fecha. No registra a quién ya se avisó: la lista sale completa cada vez.
     *
     * <p>El nombre y el teléfono salen de la cuenta del cliente, o del propio turno si lo cargó el admin. Los
     * clientes se leen en una sola consulta, igual que los nombres de los servicios.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404).
     */
    @Transactional(readOnly = true)
    public CancellationNoticesResponse listCancellationNotices(UUID companyId) {
        ZoneId zone = companyService.zoneOf(companyId);
        List<Appointment> cancelled =
                appointmentRepository
                        .findAllByCompanyIdAndStatusAndCancellationReasonAndStartDateTimeAfterOrderByStartDateTimeAsc(
                                companyId, AppointmentStatus.CANCELLED, COMPANY_DEACTIVATED_REASON, Instant.now(clock));
        Map<UUID, String> serviceNames = catalogService.list(companyId).stream()
                .collect(Collectors.toMap(ServiceResponse::id, ServiceResponse::name));
        Set<UUID> clientIds = cancelled.stream()
                .map(Appointment::getClientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ClientResponse> clients = userService.findClients(companyId, clientIds).stream()
                .collect(Collectors.toMap(ClientResponse::id, Function.identity()));

        List<CancellationNoticesResponse.Notice> notices = new ArrayList<>();
        for (Appointment appointment : cancelled) {
            ClientResponse client = clients.get(appointment.getClientId());
            String clientName =
                    client != null ? client.firstName() + " " + client.lastName() : appointment.getManualClientName();
            String phone = client != null ? client.phone() : appointment.getManualClientPhone();
            notices.add(new CancellationNoticesResponse.Notice(
                    appointment.getId(),
                    clientName,
                    phone,
                    serviceNames.get(appointment.getServiceId()),
                    appointment.getStartDateTime()));
        }
        return new CancellationNoticesResponse(zone.getId(), notices);
    }

    /**
     * Cancela un turno a pedido de su cliente, hasta el plazo configurado antes de que empiece (US-08.1). El lock de
     * la empresa evita que dos pedidos simultáneos lean el turno como pendiente. No publica un aviso: el admin avisa
     * desde su panel (ver {@code docs/contratos-api.md}, Paso 10).
     *
     * @param companyId empresa de la sesión.
     * @param clientId cliente de la sesión: nunca uno que llegue en la request.
     * @throws ApiException {@code COMPANY_INACTIVE} (403), {@code APPOINTMENT_NOT_FOUND} (404),
     *     {@code APPOINTMENT_NOT_PENDING} (409) o {@code CANCELLATION_DEADLINE_PASSED} (400).
     */
    @Transactional
    public ClientAppointmentResponse cancelByClient(UUID companyId, UUID clientId, UUID appointmentId) {
        companyService.lockActive(companyId);
        Appointment appointment = requirePendingOfClient(companyId, clientId, appointmentId);
        PlatformSettingsResponse settings = platformSettingsService.get();
        Instant now = Instant.now(clock);
        if (isPastClientDeadline(appointment, settings, now)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CANCELLATION_DEADLINE_PASSED",
                    "El turno se puede cancelar hasta %d horas antes".formatted(settings.cancellationDeadlineHours()));
        }

        markCancelled(appointment, CancelledBy.CLIENT, null, now);
        return toResponse(appointment, serviceName(companyId, appointment), settings, now);
    }

    /**
     * Cancela un turno de la empresa a pedido del admin (US-09.3), sin el plazo del cliente: también uno que ya
     * empezó y el job de cierre todavía no completó, por ejemplo si el cliente no vino. Usa el mismo lock de la
     * empresa que la cancelación del cliente, así que los dos no pueden cancelar a la vez el mismo turno. No publica un
     * aviso: el admin avisa desde su panel (US-10.2).
     *
     * @param companyId empresa de la sesión.
     * @param reason motivo opcional; vacío o en blanco se guarda sin motivo.
     * @throws ApiException {@code COMPANY_INACTIVE} (403), {@code APPOINTMENT_NOT_FOUND} (404) o
     *     {@code APPOINTMENT_NOT_PENDING} (409).
     */
    @Transactional
    public AdminAppointmentResponse cancelByCompany(UUID companyId, UUID appointmentId, String reason) {
        companyService.lockActive(companyId);
        Appointment appointment = requirePending(appointmentRepository.findByIdAndCompanyId(appointmentId, companyId));
        markCancelled(appointment, CancelledBy.COMPANY, blankToNull(reason), Instant.now(clock));
        return toAdminResponses(companyId, List.of(appointment)).getFirst();
    }

    // Un motivo vacío o con solo espacios se guarda como "sin motivo"; si tiene texto, sin los espacios de los bordes.
    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    /**
     * Horarios a los que el cliente puede reprogramar su turno en esa fecha local de la empresa (US-08.2). No valida
     * el plazo ni el tope: eso lo informa {@code canReschedule} y lo rechaza la reprogramación.
     *
     * @param companyId empresa de la sesión.
     * @param clientId cliente de la sesión: nunca uno que llegue en la request.
     * @throws ApiException {@code APPOINTMENT_NOT_FOUND} (404) o {@code APPOINTMENT_NOT_PENDING} (409).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse rescheduleAvailability(
            UUID companyId, UUID clientId, UUID appointmentId, LocalDate date) {
        Appointment appointment = requirePendingOfClient(companyId, clientId, appointmentId);
        return new AvailabilityResponse(date, availabilityService.rescheduleStarts(appointment, date));
    }

    /**
     * Reprograma un turno a pedido de su cliente (US-08.2): actualiza el mismo turno, guarda el inicio anterior y suma
     * una reprogramación. Sigue {@code PENDING}. Tiene el mismo plazo que cancelar (regla 16), y el nuevo horario pasa
     * por las mismas validaciones que una reserva, con el mismo lock de la empresa. No publica un aviso, igual que la
     * cancelación.
     *
     * @param companyId empresa de la sesión.
     * @param clientId cliente de la sesión: nunca uno que llegue en la request.
     * @throws ApiException {@code COMPANY_INACTIVE} (403), {@code APPOINTMENT_NOT_FOUND} (404),
     *     {@code APPOINTMENT_NOT_PENDING} (409), {@code RESCHEDULE_DEADLINE_PASSED} (400),
     *     {@code RESCHEDULE_LIMIT_REACHED} (409), {@code BOOKING_TOO_SOON} (400), {@code BOOKING_TOO_FAR} (400) o
     *     {@code SLOT_UNAVAILABLE} (409).
     */
    @Transactional
    public ClientAppointmentResponse rescheduleByClient(
            UUID companyId, UUID clientId, UUID appointmentId, RescheduleAppointmentRequest request) {
        companyService.lockActive(companyId);
        Appointment appointment = requirePendingOfClient(companyId, clientId, appointmentId);
        PlatformSettingsResponse settings = platformSettingsService.get();
        Instant now = Instant.now(clock);
        if (isPastClientDeadline(appointment, settings, now)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RESCHEDULE_DEADLINE_PASSED",
                    "El turno se puede reprogramar hasta %d horas antes"
                            .formatted(settings.cancellationDeadlineHours()));
        }
        if (isRescheduleLimitReached(appointment, settings)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RESCHEDULE_LIMIT_REACHED",
                    "El turno ya se reprogramó la cantidad máxima de veces (%d)"
                            .formatted(settings.maxRescheduleCount()));
        }
        ZoneId zone = companyService.zoneOf(companyId);
        Instant start = request.startDateTime();
        LocalDate date = start.atZone(zone).toLocalDate();
        requireWithinBookingWindow(start, date, zone);
        if (!availabilityService.rescheduleStarts(appointment, date).contains(start)) {
            throw slotUnavailable();
        }

        appointment.setPreviousStartDateTime(appointment.getStartDateTime());
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(appointment.getDurationMinutesSnapshot())));
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        // Escribe en el acto, igual que la reserva: la constraint excl_appointment_overlap salta acá.
        appointmentRepository.saveAndFlush(appointment);
        return toResponse(appointment, serviceName(companyId, appointment), settings, now);
    }

    // Un turno pendiente del cliente. El de otro cliente o de otra empresa da el mismo 404 que uno inexistente.
    private Appointment requirePendingOfClient(UUID companyId, UUID clientId, UUID appointmentId) {
        return requirePending(
                appointmentRepository.findByIdAndCompanyIdAndClientId(appointmentId, companyId, clientId));
    }

    // El turno buscado, si existe y sigue pendiente. Lo usan el cliente y el admin, con los mismos errores.
    private static Appointment requirePending(Optional<Appointment> found) {
        Appointment appointment = found.orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "El turno no existe"));
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_NOT_PENDING", "El turno ya no está pendiente");
        }
        return appointment;
    }

    // Incluye los servicios inactivos: un turno ya reservado sigue mostrando el suyo.
    private String serviceName(UUID companyId, Appointment appointment) {
        return catalogService
                .requireOwnedService(companyId, appointment.getServiceId())
                .getName();
    }

    // canCancel, canReschedule y rescheduleBlockedReason los calcula el back: el cliente no puede leer
    // PlatformSettings (D1 de contratos-api.md).
    private static ClientAppointmentResponse toResponse(
            Appointment appointment, String serviceName, PlatformSettingsResponse settings, Instant now) {
        return ClientAppointmentResponse.from(
                appointment,
                serviceName,
                canCancel(appointment, settings, now),
                rescheduleBlockedReason(appointment, settings, now));
    }

    private static boolean canCancel(Appointment appointment, PlatformSettingsResponse settings, Instant now) {
        return appointment.getStatus() == AppointmentStatus.PENDING
                && !isPastClientDeadline(appointment, settings, now);
    }

    // Null si se puede reprogramar. Va en el mismo orden en que rechaza rescheduleByClient.
    private static RescheduleBlockedReason rescheduleBlockedReason(
            Appointment appointment, PlatformSettingsResponse settings, Instant now) {
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            return RescheduleBlockedReason.NOT_PENDING;
        }
        if (isPastClientDeadline(appointment, settings, now)) {
            return RescheduleBlockedReason.DEADLINE_PASSED;
        }
        if (isRescheduleLimitReached(appointment, settings)) {
            return RescheduleBlockedReason.LIMIT_REACHED;
        }
        return null;
    }

    // Plazo para que el cliente cancele o reprograme: el mismo para las dos cosas (regla 16).
    private static boolean isPastClientDeadline(
            Appointment appointment, PlatformSettingsResponse settings, Instant now) {
        Instant deadline = appointment.getStartDateTime().minus(Duration.ofHours(settings.cancellationDeadlineHours()));
        return now.isAfter(deadline);
    }

    private static boolean isRescheduleLimitReached(Appointment appointment, PlatformSettingsResponse settings) {
        return appointment.getRescheduleCount() >= settings.maxRescheduleCount();
    }

    private static void markCancelled(Appointment appointment, CancelledBy cancelledBy, String reason, Instant now) {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancelledAt(now);
        appointment.setCancellationReason(reason);
    }

    /** Total de turnos de toda la plataforma, en cualquier estado, para el dashboard del superadmin (US-04.6). */
    @Transactional(readOnly = true)
    public long countAll() {
        return appointmentRepository.count();
    }

    /**
     * Pasa a {@link AppointmentStatus#COMPLETED} los turnos pendientes que ya terminaron (US-11.1). Lo llama el
     * job de cierre.
     *
     * @return cuántos turnos se completaron.
     */
    @Transactional
    public int completeFinished() {
        return appointmentRepository.completeFinished(Instant.now(clock));
    }
}
