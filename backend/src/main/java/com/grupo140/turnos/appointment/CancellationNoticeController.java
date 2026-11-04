package com.grupo140.turnos.appointment;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos de la desactivación de una empresa, en el panel del superadmin. El prefijo {@code /api/superadmin} ya exige
 * el rol {@code SUPERADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/superadmin/companies/{id}")
public class CancellationNoticeController {

    private final AppointmentService appointmentService;

    public CancellationNoticeController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Turnos futuros cancelados por la baja, para avisar a cada cliente por WhatsApp (US-04.4). */
    @GetMapping("/cancellation-notices")
    public CancellationNoticesResponse cancellationNotices(@PathVariable UUID id) {
        return appointmentService.listCancellationNotices(id);
    }
}
