package com.grupo140.turnos.appointment;

import com.grupo140.turnos.company.CompanyDeactivatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cancela los turnos pendientes de una empresa cuando se desactiva (US-04.4).
 *
 * <p>Escucha {@link CompanyDeactivatedEvent}, que publica {@code CompanyService}. Así {@code company} no depende
 * de {@code appointment} (ver la regla de dependencias del README). {@code @EventListener} corre en el momento,
 * dentro de la misma transacción que la desactivación: si la cancelación falla, la empresa tampoco queda
 * desactivada.
 */
@Component
public class CompanyDeactivationListener {

    private final AppointmentService appointmentService;

    public CompanyDeactivationListener(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @EventListener
    public void onCompanyDeactivated(CompanyDeactivatedEvent event) {
        appointmentService.cancelPendingByCompany(event.companyId());
    }
}
