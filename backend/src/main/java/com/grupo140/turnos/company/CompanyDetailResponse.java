package com.grupo140.turnos.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.grupo140.turnos.user.UserStatus;

/**
 * Detalle de una empresa para el superadmin: los mismos campos que {@link CompanyResponse}, más su admin.
 * Con el estado del admin, el front decide si muestra el botón de reenviar el link de activación (US-04.8).
 *
 * <p>{@code @JsonUnwrapped} deja los campos de la empresa al mismo nivel que {@code admin} en el JSON, en
 * lugar de anidarlos dentro de {@code "company"}.
 */
public record CompanyDetailResponse(@JsonUnwrapped CompanyResponse company, AdminSummary admin) {

    /** Lo que el front necesita del admin. Es {@code null} si la empresa no tiene admin (no debería pasar). */
    public record AdminSummary(String email, UserStatus status) {}
}
