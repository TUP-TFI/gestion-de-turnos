package com.grupo140.turnos.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Servicio tal como lo devuelve la API. Coincide con el tipo {@code Service} del frontend
 * ({@code frontend/src/types/Service.ts}).
 */
public record ServiceResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        ServiceStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getCompanyId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getStatus(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
