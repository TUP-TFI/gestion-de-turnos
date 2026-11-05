package com.grupo140.turnos.appointment;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Reprogramación de un turno (US-08.2). El nuevo inicio es un instante en UTC, igual que en la reserva. */
public record RescheduleAppointmentRequest(
        @NotNull(message = "El horario es obligatorio") Instant startDateTime) {}
