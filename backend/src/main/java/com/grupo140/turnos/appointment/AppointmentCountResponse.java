package com.grupo140.turnos.appointment;

/**
 * Cantidad de turnos para un aviso del panel: los pendientes futuros de un servicio (US-05.2) o los que quedan
 * fuera de un horario propuesto (US-05.4).
 */
public record AppointmentCountResponse(long count) {}
