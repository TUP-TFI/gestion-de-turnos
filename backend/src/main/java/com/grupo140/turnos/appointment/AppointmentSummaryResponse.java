package com.grupo140.turnos.appointment;

import java.util.List;

/**
 * Resumen del dashboard del admin (US-09.1), sin turnos cancelados. Los dos van del más cercano al más lejano.
 *
 * @param today los turnos de hoy, en la fecha local de la empresa.
 * @param upcoming los próximos 5 turnos después de hoy (D4 de {@code docs/contratos-api.md}).
 */
public record AppointmentSummaryResponse(
        List<AdminAppointmentResponse> today, List<AdminAppointmentResponse> upcoming) {}
