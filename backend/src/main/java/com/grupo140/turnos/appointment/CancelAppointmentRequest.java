package com.grupo140.turnos.appointment;

import jakarta.validation.constraints.Size;

/** Cancelación de un turno por el admin (US-09.3). El motivo es opcional: vacío o en blanco se guarda sin motivo. */
public record CancelAppointmentRequest(
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
        String reason) {}
