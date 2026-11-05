package com.grupo140.turnos.user;

import java.util.UUID;

/**
 * Un cliente de la empresa tal como lo ve su admin (US-09.4 y US-09.7). Solo trae datos de contacto: nunca la
 * contraseña ni datos de sesión.
 */
public record ClientResponse(UUID id, String firstName, String lastName, String email, String phone) {

    static ClientResponse from(User user) {
        return new ClientResponse(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone());
    }
}
