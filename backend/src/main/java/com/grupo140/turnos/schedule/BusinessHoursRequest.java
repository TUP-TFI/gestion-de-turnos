package com.grupo140.turnos.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Cuerpo de {@code PUT /api/company/business-hours} (US-05.3) y del conteo de turnos fuera de rango
 * (US-05.4). Es la semana completa: reemplaza todas las franjas de la empresa, y un día sin franjas queda
 * cerrado. Que las franjas no se superpongan lo valida {@link ScheduleService#validate}.
 */
public record BusinessHoursRequest(
        @NotNull(message = "Las franjas horarias son obligatorias")
        List<@NotNull(message = "Las franjas horarias no pueden estar vacías") @Valid Slot> hours) {

    /** Una franja de atención. Las horas se interpretan en la zona horaria de la empresa. */
    public record Slot(
            @NotNull(message = "El día de la semana es obligatorio")
            DayOfWeek dayOfWeek,

            @NotNull(message = "La hora de inicio es obligatoria")
            LocalTime startTime,

            @NotNull(message = "La hora de fin es obligatoria")
            LocalTime endTime,

            // 1440 minutos = 24 h: el intervalo entre turnos no puede superar un día.
            @NotNull(message = "El intervalo es obligatorio")
            @Positive(message = "El intervalo debe ser mayor a 0 minutos")
            @Max(value = 1440, message = "El intervalo no puede superar los 1440 minutos")
            Integer intervalMinutes) {}
}
