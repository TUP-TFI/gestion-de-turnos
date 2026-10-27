package com.grupo140.turnos.schedule;

import java.time.LocalTime;

/**
 * Una franja de atención de una fecha concreta: desde {@code start} hasta {@code end}, con un horario de
 * inicio posible cada {@code intervalMinutes}. Viene del horario semanal o de la excepción de esa fecha.
 */
public record DayWindow(LocalTime start, LocalTime end, int intervalMinutes) {}
