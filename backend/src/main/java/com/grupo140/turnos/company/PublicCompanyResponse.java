package com.grupo140.turnos.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

/**
 * Datos de la empresa que muestra su página pública (US-06.1), sin login. Incluye {@code timezone} para que el
 * front pase a la hora local los horarios, que viajan en UTC.
 */
public record PublicCompanyResponse(
        // Solo para buscar los servicios de la empresa: el visitante llega por el slug y no lo necesita.
        @JsonIgnore UUID id,
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
        CompanyStatus status) {

    static PublicCompanyResponse from(Company company) {
        return new PublicCompanyResponse(
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
                company.getStatus());
    }
}
