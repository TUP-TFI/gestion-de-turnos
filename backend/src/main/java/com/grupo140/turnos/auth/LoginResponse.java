package com.grupo140.turnos.auth;

import java.util.UUID;

/**
 * Respuesta de un login exitoso: el JWT y los datos que el frontend muestra de la sesión.
 */
public record LoginResponse(String token, LoggedUser user) {

    /** Usuario autenticado. {@code companyId} es nulo para el superadmin. */
    public record LoggedUser(UUID id, String firstName, String lastName, String email, String role, UUID companyId) {}
}
