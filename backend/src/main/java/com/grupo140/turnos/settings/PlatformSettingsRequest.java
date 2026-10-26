package com.grupo140.turnos.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cuerpo de {@code PUT /api/superadmin/settings} (US-04.9). Los cuatro valores son obligatorios y siguen
 * las mismas reglas que los {@code CHECK} de la tabla {@code platform_settings}. Además llevan un tope
 * máximo, que solo se valida acá: la base no lo exige.
 */
public record PlatformSettingsRequest(
        @NotNull(message = "La anticipación mínima es obligatoria")
        @Positive(message = "La anticipación mínima debe ser mayor a 0 minutos")
        @Max(value = 1440, message = "La anticipación mínima no puede superar los 1440 minutos (24 horas)")
        Integer minBookingNoticeMinutes,

        @NotNull(message = "El plazo de cancelación es obligatorio")
        @Positive(message = "El plazo de cancelación debe ser mayor a 0 horas")
        @Max(value = 168, message = "El plazo de cancelación no puede superar las 168 horas (7 días)")
        Integer cancellationDeadlineHours,

        @NotNull(message = "El máximo de reprogramaciones es obligatorio")
        @PositiveOrZero(message = "El máximo de reprogramaciones no puede ser negativo")
        @Max(value = 10, message = "El máximo de reprogramaciones no puede superar 10")
        Integer maxRescheduleCount,

        @NotNull(message = "La anticipación máxima es obligatoria")
        @Positive(message = "La anticipación máxima debe ser mayor a 0 días")
        @Max(value = 365, message = "La anticipación máxima no puede superar los 365 días")
        Integer maxBookingAdvanceDays) {}
