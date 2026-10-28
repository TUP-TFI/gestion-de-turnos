package com.grupo140.turnos.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Servicio tal como lo ve un visitante en la página pública (US-06.1). Solo trae lo que se muestra; el
 * {@code id} lo usa el front para pedir los horarios disponibles (US-06.2).
 */
public record PublicServiceResponse(
        UUID id, String name, String description, BigDecimal price, Integer durationMinutes) {

    static PublicServiceResponse from(Service service) {
        return new PublicServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes());
    }
}
