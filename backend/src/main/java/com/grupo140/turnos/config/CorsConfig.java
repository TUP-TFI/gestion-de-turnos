package com.grupo140.turnos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend desplegado pueda llamar a la API (T-01.3).
 * <p>
 * Los origenes permitidos se configuran por perfil con {@code cors.allowed-origins}:
 * en {@code dev} apunta al servidor de Vite y en {@code prod} sale de la variable
 * de entorno {@code CORS_ALLOWED_ORIGINS}.
 * <p>
 * El valor es un origen ({@code https://host}), sin path: es lo que el navegador
 * manda en el header {@code Origin}. La barra final Spring la ignora al comparar.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
