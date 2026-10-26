package com.grupo140.turnos.user;

import java.util.UUID;

/**
 * Datos de un usuario (superadmin, admin de empresa o cliente) que necesita {@code auth} para validar el
 * login. Evita que la entidad {@link User} salga del feature.
 *
 * @param passwordHash hash BCrypt; nulo mientras el estado sea {@link UserStatus#PENDING_ACTIVATION}.
 * @param companyId empresa del usuario; nulo para el superadmin.
 */
public record UserCredentials(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        UUID companyId,
        UserStatus status,
        String passwordHash) {}
