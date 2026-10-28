package com.grupo140.turnos.appointment;

import com.grupo140.turnos.common.security.SessionUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de turnos del cliente. El prefijo {@code /api/client} ya exige el rol {@code CLIENT} (ver
 * {@code SecurityConfig}), y el cliente y la empresa salen siempre de la sesión.
 */
@RestController
@RequestMapping("/api/client/appointments")
public class ClientAppointmentController {

    private final AppointmentService appointmentService;

    public ClientAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** "Mis turnos": los del cliente, del más reciente al más antiguo. Se puede filtrar por estado (US-07.6). */
    @GetMapping
    public List<ClientAppointmentResponse> list(
            SessionUser session, @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.listForClient(session.companyId(), session.userId(), status);
    }
}
