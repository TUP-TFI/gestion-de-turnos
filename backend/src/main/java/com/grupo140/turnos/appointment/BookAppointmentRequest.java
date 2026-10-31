package com.grupo140.turnos.appointment;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Reserva de un turno (US-07.3). El inicio es un instante en UTC: lo convierte el front desde la hora local. */
public record BookAppointmentRequest(
        @NotNull(message = "El servicio es obligatorio") UUID serviceId,
        @NotNull(message = "El horario es obligatorio") Instant startDateTime) {}
