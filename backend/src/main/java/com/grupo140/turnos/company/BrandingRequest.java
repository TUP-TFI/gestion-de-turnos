package com.grupo140.turnos.company;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/company/branding} (US-05.7). Los dos campos son opcionales a propósito: {@code null}
 * borra el valor, y la página pública usa el estilo por defecto.
 *
 * <p>El logo tiene la misma validación que en el alta y la edición del superadmin
 * ({@link CreateCompanyRequest.CompanyData}), porque los dos escriben el mismo campo y no pueden aceptar valores
 * distintos. El color sigue el {@code CHECK chk_company_color} de la base.
 */
public record BrandingRequest(
        @Size(max = 500, message = "La dirección del logo no puede superar los 500 caracteres")
        String logoUrl,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB")
        String primaryColor) {}
