package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Horarios disponibles de un servicio en una fecha (US-06.2). {@code date} es la fecha local de la empresa y
 * {@code starts} los inicios libres, en UTC y ordenados: el front los muestra con la zona horaria de la empresa.
 */
public record AvailabilityResponse(LocalDate date, List<Instant> starts) {}
