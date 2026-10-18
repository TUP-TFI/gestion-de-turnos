package com.grupo140.turnos.settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cuerpo de {@code PUT /api/superadmin/settings} (US-04.9). Los cuatro valores son obligatorios y siguen
 * las mismas reglas que los {@code CHECK} de la tabla {@code platform_settings}.
 */
public record PlatformSettingsRequest(
        @NotNull(message = "La anticipación mínima es obligatoria")
        @Positive(message = "La anticipación mínima debe ser mayor a 0 minutos")
        Integer minBookingNoticeMinutes,

        @NotNull(message = "El plazo de cancelación es obligatorio")
        @Positive(message = "El plazo de cancelación debe ser mayor a 0 horas")
        Integer cancellationDeadlineHours,

        @NotNull(message = "El máximo de reprogramaciones es obligatorio")
        @PositiveOrZero(message = "El máximo de reprogramaciones no puede ser negativo")
        Integer maxRescheduleCount,

        @NotNull(message = "La anticipación máxima es obligatoria")
        @Positive(message = "La anticipación máxima debe ser mayor a 0 días")
        Integer maxBookingAdvanceDays) {}
