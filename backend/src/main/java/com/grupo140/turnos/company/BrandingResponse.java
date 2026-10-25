package com.grupo140.turnos.company;

/**
 * Marca de la empresa tal como la devuelve la API: el logo (una URL cargada a mano, sin storage) y el color
 * primario en formato {@code #RRGGBB}. Cualquiera de los dos puede ser {@code null}.
 */
public record BrandingResponse(String logoUrl, String primaryColor) {

    static BrandingResponse from(Company company) {
        return new BrandingResponse(company.getLogoUrl(), company.getPrimaryColor());
    }
}
