package com.grupo140.turnos.appointment;

import com.grupo140.turnos.common.PageResponse;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.common.security.SessionUser;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleExceptionRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de turnos del panel de la empresa. El prefijo {@code /api/company} ya exige el rol
 * {@code COMPANY_ADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/company")
public class AppointmentAdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;

    public AppointmentAdminController(AppointmentService appointmentService, AvailabilityService availabilityService) {
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
    }

    /** Carga manual de un turno, para un cliente con cuenta o sin cuenta (US-09.5 y US-09.6). */
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAppointmentResponse create(SessionUser session, @Valid @RequestBody ManualAppointmentRequest request) {
        return appointmentService.bookManual(session.companyId(), request);
    }

    /**
     * Horarios libres de un servicio en una fecha local de la empresa, para la carga manual (D3 de
     * {@code contratos-api.md}). Son los mismos que ve la página pública, con la empresa de la sesión en vez del slug.
     */
    @GetMapping("/services/{id}/availability")
    public AvailabilityResponse availability(SessionUser session, @PathVariable UUID id, @RequestParam LocalDate date) {
        return new AvailabilityResponse(date, availabilityService.availableStarts(session.companyId(), id, date));
    }

    /** Agenda: todos los turnos de la empresa, paginados y con filtros opcionales (US-09.2). */
    @GetMapping("/appointments")
    public PageResponse<AdminAppointmentResponse> list(
            SessionUser session,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) AppointmentOrigin origin,
            @RequestParam(required = false) Boolean rescheduled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La página o el tamaño pedidos no son válidos");
        }
        return appointmentService.listForCompany(session.companyId(), status, origin, rescheduled, page, size);
    }

    /** Resumen del dashboard: los turnos de hoy y los próximos 5, sin cancelados (US-09.1). */
    @GetMapping("/appointments/summary")
    public AppointmentSummaryResponse summary(SessionUser session) {
        return appointmentService.summary(session.companyId());
    }

    /** Cancela un turno de la empresa, sin el plazo del cliente y con un motivo opcional (US-09.3). */
    @PostMapping("/appointments/{id}/cancel")
    public AdminAppointmentResponse cancel(
            SessionUser session,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelAppointmentRequest request) {
        // Sin body es lo mismo que sin motivo.
        String reason = request != null ? request.reason() : null;
        return appointmentService.cancelByCompany(session.companyId(), id, reason);
    }

    /** Cantidad de turnos pendientes futuros de un servicio, para avisar antes de desactivarlo (US-05.2). */
    @GetMapping("/services/{id}/future-appointments")
    public AppointmentCountResponse futureAppointments(SessionUser session, @PathVariable UUID id) {
        return new AppointmentCountResponse(appointmentService.countFutureByService(session.companyId(), id));
    }

    /** Cantidad de turnos pendientes futuros fuera de un horario propuesto, sin guardarlo (US-05.4). */
    @PostMapping("/business-hours/out-of-range-appointments")
    public AppointmentCountResponse outOfRangeAppointments(
            SessionUser session, @Valid @RequestBody BusinessHoursRequest request) {
        return new AppointmentCountResponse(appointmentService.countOutOfRange(session.companyId(), request));
    }

    /** Cantidad de turnos pendientes futuros que quedan fuera de una excepción propuesta, sin guardarla (US-05.6). */
    @PostMapping("/schedule-exceptions/affected-appointments")
    public AppointmentCountResponse affectedAppointments(
            SessionUser session, @Valid @RequestBody ScheduleExceptionRequest request) {
        return new AppointmentCountResponse(appointmentService.countAffectedByException(session.companyId(), request));
    }
}
