package com.grupo140.turnos.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Carga manual de un turno por el admin (US-09.5 y US-09.6). Lleva una sola identidad: {@code clientId} si el cliente
 * tiene cuenta, o nombre y teléfono si no. Que venga exactamente una lo valida el service, con
 * {@code INVALID_CLIENT_IDENTITY}. Los topes de largo son los de las columnas de la base.
 */
public record ManualAppointmentRequest(
        @NotNull(message = "El servicio es obligatorio") UUID serviceId,
        @NotNull(message = "El horario es obligatorio") Instant startDateTime,
        UUID clientId,

        @Size(max = 160, message = "El nombre no puede superar los 160 caracteres")
        String manualClientName,

        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String manualClientPhone) {}
