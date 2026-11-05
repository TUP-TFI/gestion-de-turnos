package com.grupo140.turnos.appointment;

/** Por qué el cliente no puede reprogramar un turno (US-08.3). El front muestra la leyenda según el motivo. */
public enum RescheduleBlockedReason {
    // Ya está cancelado o completado.
    NOT_PENDING,
    // Pasó el plazo de cancellationDeadlineHours antes del turno.
    DEADLINE_PASSED,
    // Ya se reprogramó maxRescheduleCount veces.
    LIMIT_REACHED
}
