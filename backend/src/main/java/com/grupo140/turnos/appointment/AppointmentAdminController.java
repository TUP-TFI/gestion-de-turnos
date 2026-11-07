package com.grupo140.turnos.appointment;

import com.grupo140.turnos.common.PageResponse;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.common.security.SessionUser;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleExceptionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    public AppointmentAdminController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
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
