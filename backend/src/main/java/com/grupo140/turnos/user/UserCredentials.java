package com.grupo140.turnos.user;

import java.util.UUID;

/**
 * Datos de un usuario del panel (superadmin o admin de empresa) que necesita {@code auth} para validar
 * el login. Evita que la entidad {@link User} salga del feature.
 *
 * @param passwordHash hash BCrypt; nulo mientras el estado sea {@link UserStatus#PENDING_ACTIVATION}.
 * @param companyId empresa del usuario; nulo para el superadmin.
 */
public record PanelUserCredentials(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        UUID companyId,
        UserStatus status,
        String passwordHash) {}
