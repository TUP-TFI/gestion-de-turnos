package com.grupo140.turnos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/auth/login}. El tope de 72 caracteres es el límite de BCrypt.
 */
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
        String password) {}
