package com.grupo140.turnos.appointment;

import com.grupo140.turnos.common.security.SessionUser;
import com.grupo140.turnos.schedule.BusinessHoursRequest;
import com.grupo140.turnos.schedule.ScheduleExceptionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de turnos del panel de la empresa. El prefijo {@code /api/company} ya exige el rol
 * {@code COMPANY_ADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/company")
public class AppointmentAdminController {

    private final AppointmentService appointmentService;

    public AppointmentAdminController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
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
