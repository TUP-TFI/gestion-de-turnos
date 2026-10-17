package com.grupo140.turnos.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST} y {@code PUT /api/company/services} (US-05.1). El estado no viene en el pedido:
 * el alta lo deja en {@code ACTIVE} y se cambia con los endpoints de activar y desactivar (US-05.2). Los
 * topes de largo y de dígitos son los de las columnas de la base.
 */
public record ServiceRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 120, message = "El nombre del servicio no puede superar los 120 caracteres")
        String name,

        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 8 dígitos y 2 decimales")
        BigDecimal price,

        // 1440 minutos = 24 h: un turno no puede durar más que un día.
        @NotNull(message = "La duración es obligatoria")
        @Positive(message = "La duración debe ser mayor a 0 minutos")
        @Max(value = 1440, message = "La duración no puede superar los 1440 minutos")
        Integer durationMinutes) {}
