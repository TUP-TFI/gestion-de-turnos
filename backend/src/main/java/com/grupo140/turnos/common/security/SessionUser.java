package com.grupo140.turnos.common.security;

import java.util.UUID;

/**
 * Usuario autenticado de la request, armado desde el JWT (US-03.9). Los controllers lo reciben como
 * parámetro y le pasan {@link #companyId()} al service: el {@code companyId} nunca sale del body ni de
 * un query param.
 *
 * <p>El rol va como {@code String} porque {@code common} no puede importar el enum de {@code user}.
 *
 * @param userId id del usuario ({@code sub} del token).
 * @param role nombre del rol ({@code SUPERADMIN}, {@code COMPANY_ADMIN} o {@code CLIENT}).
 * @param companyId empresa del usuario; nulo para el superadmin.
 */
public record SessionUser(UUID userId, String role, UUID companyId) {}
