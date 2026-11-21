package com.grupo140.turnos.schedule;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Una franja de atención de una fecha concreta: desde {@code start} hasta {@code end}, con un horario de
 * inicio posible cada {@code intervalMinutes}. Viene del horario semanal o de la excepción de esa fecha.
 */
public record DayWindow(LocalTime start, LocalTime end, int intervalMinutes) {

    /**
     * Fin de una franja que cierra a medianoche. La base exige que el fin sea posterior al inicio
     * ({@code chk_hours_range}), así que no se puede guardar 00:00 como fin: la pantalla pide 23:59, y el
     * cálculo lo toma como las 24:00. Así un día abierto las 24 horas (00:00 a 23:59) no pierde el último turno.
     */
    public static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    /** Si una franja que termina a esta hora cierra a medianoche. */
    public static boolean endsAtMidnight(LocalTime end) {
        return END_OF_DAY.equals(end);
    }

    /** Minutos de la franja. Si termina a las 23:59, cuenta hasta la medianoche. */
    public long minutes() {
        long minutes = Duration.between(start, end).toMinutes();
        return endsAtMidnight(end) ? minutes + 1 : minutes;
    }
}
