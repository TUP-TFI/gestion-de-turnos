package com.grupo140.turnos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/auth/activate} (US-03.4). {@code token} es el valor en claro que viaja en
 * el link. El tope de 72 caracteres de la contraseña es el límite de BCrypt.
 */
public record ActivationRequest(
        @NotBlank(message = "El token es obligatorio") String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password) {}
