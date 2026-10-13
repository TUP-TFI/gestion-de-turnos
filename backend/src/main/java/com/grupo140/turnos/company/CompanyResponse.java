package com.grupo140.turnos.company;

import java.time.Instant;
import java.util.UUID;

/**
 * Empresa tal como la devuelve la API. Coincide con el tipo {@code Company} del frontend
 * ({@code frontend/src/types/Company.ts}).
 */
public record CompanyResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String address,
        String phone,
        String contactEmail,
        CompanyCategory category,
        String timezone,
        String logoUrl,
        String primaryColor,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getDescription(),
                company.getAddress(),
                company.getPhone(),
                company.getContactEmail(),
                company.getCategory(),
                company.getTimezone(),
                company.getLogoUrl(),
                company.getPrimaryColor(),
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
