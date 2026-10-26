package com.grupo140.turnos.company;

/**
 * Marca de la empresa tal como la devuelve la API: el logo (una URL cargada a mano, sin storage) y el color
 * primario en formato {@code #RRGGBB}. Cualquiera de los dos puede ser {@code null}. Incluye el slug, de solo
 * lectura, para que el panel arme el link a la página pública: la sesión del admin solo trae el {@code companyId}.
 */
public record BrandingResponse(String slug, String logoUrl, String primaryColor) {

    static BrandingResponse from(Company company) {
        return new BrandingResponse(company.getSlug(), company.getLogoUrl(), company.getPrimaryColor());
    }
}
