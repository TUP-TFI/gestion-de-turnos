package com.grupo140.turnos.config;

import com.grupo140.turnos.common.error.ApiError;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad de la API (US-03.9): sesión stateless con JWT, autorización por prefijo de ruta según el
 * rol y errores 401/403 en el formato {@link ApiError}.
 *
 * <p>Convención de rutas: {@code /api/public/**} sin sesión, {@code /api/superadmin/**} solo
 * {@code SUPERADMIN}, {@code /api/company/**} solo {@code COMPANY_ADMIN} y {@code /api/client/**} solo
 * {@code CLIENT}. Todo lo demás exige estar autenticado.
 *
 * <p>También define el {@link PasswordEncoder} único del sistema: lo usan el login (US-03.1), la
 * activación (US-03.4) y el seed de dev (T-02.4). Si cada uno hasheara por su cuenta, las contraseñas
 * sembradas no validarían. BCrypt genera hashes de 60 caracteres ({@code app_user.password} es
 * {@code varchar(100)}) y rechaza contraseñas de más de 72 bytes: el DTO de entrada limita el largo antes.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, ex) ->
                writeError(response, jsonMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Necesitás iniciar sesión");
        AccessDeniedHandler deniedHandler = (request, response, ex) -> writeError(
                response, jsonMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tenés permisos para esta operación");

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Reusa CorsConfig: el filtro de seguridad corre antes que el MVC.
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/ping", "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login")
                        .permitAll()
                        .requestMatchers("/api/public/**")
                        .permitAll()
                        .requestMatchers("/api/superadmin/**")
                        .hasRole("SUPERADMIN")
                        .requestMatchers("/api/company/**")
                        .hasRole("COMPANY_ADMIN")
                        .requestMatchers("/api/client/**")
                        .hasRole("CLIENT")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    // El claim "role" del token pasa a la authority ROLE_<rol> que espera hasRole(...).
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Reloj inyectable para que los tests fijen "ahora" al emitir el token.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static SecretKey secretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // Los errores del filtro ocurren antes del MVC: el advice no los atrapa y hay que escribirlos acá.
    private static void writeError(
            HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), new ApiError(code, message));
    }
}
