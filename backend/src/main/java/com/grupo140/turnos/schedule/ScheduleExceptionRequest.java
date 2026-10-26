package com.grupo140.turnos.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cuerpo del alta y la edición de una excepción de calendario (US-05.6), y del conteo de turnos afectados.
 * Las horas se interpretan en la zona horaria de la empresa. Que la fecha no sea pasada y que las horas
 * correspondan a {@code isClosed} lo valida {@link ScheduleService#validateException}.
 */
public record ScheduleExceptionRequest(
        @NotNull(message = "La fecha es obligatoria") LocalDate exceptionDate,

        @NotNull(message = "Indicá si el día está cerrado") Boolean isClosed,

        // Los tres siguientes van vacíos si isClosed = true y son obligatorios si isClosed = false.
        LocalTime startTime,

        LocalTime endTime,

        // 1440 minutos = 24 h: el intervalo entre turnos no puede superar un día.
        @Positive(message = "El intervalo debe ser mayor a 0 minutos")
        @Max(value = 1440, message = "El intervalo no puede superar los 1440 minutos")
        Integer intervalMinutes,

        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
        String reason) {}
