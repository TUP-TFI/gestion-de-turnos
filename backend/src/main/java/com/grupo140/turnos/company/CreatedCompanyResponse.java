package com.grupo140.turnos.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Respuesta del alta de empresa (US-04.1): los mismos campos que {@link CompanyResponse}, más el link de activación
 * del admin. Lo usa solo el alta: el listado y el detalle no traen el link.
 *
 * <p>{@code @JsonUnwrapped} deja todo al mismo nivel en el JSON, igual que en {@link CompanyDetailResponse}.
 */
public record CreatedCompanyResponse(
        @JsonUnwrapped CompanyResponse company,
        @JsonUnwrapped ActivationLinkResponse activation) {}
