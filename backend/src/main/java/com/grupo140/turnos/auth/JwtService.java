package com.grupo140.turnos.auth;

import com.grupo140.turnos.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite los JWT de sesión del panel (HS256). La validación de cada request la hace Spring Security
 * con el {@code JwtDecoder} de {@code SecurityConfig}.
 */
@Component
public class JwtService {

    /** Audiencia de los tokens del panel (superadmin y admin de empresa). */
    static final String PANEL_AUDIENCE = "panel";

    /** Audiencia de los tokens del cliente final (US-03.6). */
    static final String CLIENT_AUDIENCE = "client";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emite un token con {@code sub = userId}, la audiencia indicada y los claims {@code role} y
     * {@code companyId}. El último se omite si es nulo (superadmin).
     *
     * <p>El decoder de {@code SecurityConfig} no valida la audiencia: lo que separa las rutas del panel de las
     * del cliente es el rol, por prefijo. La audiencia solo identifica el origen del token.
     */
    public String issue(UUID userId, String role, UUID companyId, String audience) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .claim("role", role);
        if (companyId != null) {
            claims.claim("companyId", companyId.toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
    }
}
