package com.grupo140.turnos.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un turno tal como lo ve el admin en su agenda (US-09.1 y US-09.2). Es {@code AdminAppointment} de
 * {@code docs/contratos-api.md}. Las fechas van en UTC: el panel las muestra con el {@code timezone} de la marca.
 */
public record AdminAppointmentResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        Client client,
        Instant startDateTime,
        Instant endDateTime,
        AppointmentStatus status,
        AppointmentOrigin origin,
        int rescheduleCount,
        Instant previousStartDateTime,
        BigDecimal priceSnapshot,
        int durationMinutesSnapshot,
        CancelledBy cancelledBy,
        String cancellationReason,
        Instant cancelledAt) {

    /**
     * Quién tiene el turno. Con cuenta, los datos salen de la cuenta; en un turno manual sin cuenta, del propio turno,
     * y {@code id} es {@code null}.
     */
    public record Client(UUID id, String name, String phone) {}

    static AdminAppointmentResponse from(Appointment appointment, String serviceName, Client client) {
        return new AdminAppointmentResponse(
                appointment.getId(),
                appointment.getServiceId(),
                serviceName,
                client,
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getStatus(),
                appointment.getOrigin(),
                appointment.getRescheduleCount(),
                appointment.getPreviousStartDateTime(),
                appointment.getPriceSnapshot(),
                appointment.getDurationMinutesSnapshot(),
                appointment.getCancelledBy(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt());
    }
}
