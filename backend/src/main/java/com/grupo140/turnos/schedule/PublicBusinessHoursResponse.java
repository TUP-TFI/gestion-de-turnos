package com.grupo140.turnos.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja de atención tal como la ve un visitante en la página pública. Solo trae lo que se muestra: el intervalo es
 * un dato interno de la grilla de turnos. Las horas son locales de la empresa.
 */
public record PublicBusinessHoursResponse(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

    static PublicBusinessHoursResponse from(BusinessHoursResponse hours) {
        return new PublicBusinessHoursResponse(hours.dayOfWeek(), hours.startTime(), hours.endTime());
    }
}
