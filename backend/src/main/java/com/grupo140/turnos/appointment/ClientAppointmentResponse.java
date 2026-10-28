package com.grupo140.turnos.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un turno de "Mis turnos" del cliente (US-07.6). El precio y la duración son los que se copiaron al reservar,
 * no los actuales del servicio. Las fechas van en UTC: las convierte el front a la hora local.
 */
public record ClientAppointmentResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        Instant startDateTime,
        Instant endDateTime,
        AppointmentStatus status,
        int rescheduleCount,
        BigDecimal priceSnapshot,
        int durationMinutesSnapshot) {

    static ClientAppointmentResponse from(Appointment appointment, String serviceName) {
        return new ClientAppointmentResponse(
                appointment.getId(),
                appointment.getServiceId(),
                serviceName,
                appointment.getStartDateTime(),
                appointment.getEndDateTime(),
                appointment.getStatus(),
                appointment.getRescheduleCount(),
                appointment.getPriceSnapshot(),
                appointment.getDurationMinutesSnapshot());
    }
}
