package com.grupo140.turnos.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT de sesión ({@code app.jwt.*}).
 *
 * <p>El secreto firma con HS256, que exige al menos 32 bytes. Se valida acá y no en el primer login:
 * si es corto, la app no arranca.
 *
 * @param secret secreto de firma. En {@code prod} sale de la variable {@code JWT_SECRET}.
 * @param expiration vida del token desde que se emite.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("app.jwt.secret debe tener al menos 32 bytes (JWT_SECRET en prod)");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt.expiration debe ser una duración positiva");
        }
    }
}
