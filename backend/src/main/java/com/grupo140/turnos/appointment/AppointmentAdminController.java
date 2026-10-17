package com.grupo140.turnos.appointment;

import com.grupo140.turnos.common.security.SessionUser;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de turnos del panel de la empresa. El prefijo {@code /api/company} ya exige el rol
 * {@code COMPANY_ADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/company/services")
public class AppointmentAdminController {

    private final AppointmentService appointmentService;

    public AppointmentAdminController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Cantidad de turnos pendientes futuros de un servicio, para avisar antes de desactivarlo (US-05.2). */
    @GetMapping("/{id}/future-appointments")
    public FutureAppointmentsResponse futureAppointments(SessionUser session, @PathVariable UUID id) {
        return new FutureAppointmentsResponse(appointmentService.countFutureByService(session.companyId(), id));
    }
}
