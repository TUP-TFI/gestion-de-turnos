package com.grupo140.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo140.turnos.config.JwtProperties;
import com.grupo140.turnos.config.SecurityConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtServiceTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-secret-0123456789-abcdefghijklmnop", Duration.ofHours(8));

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtDecoder decoder = securityConfig.jwtDecoder(PROPERTIES);

    // Instante real (truncado a segundos): el decoder valida exp contra el reloj del sistema.
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final JwtService jwtService =
            new JwtService(securityConfig.jwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void tokenContainsSubRoleCompanyIdAndExpiration() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Jwt jwt = decoder.decode(jwtService.issue(userId, "COMPANY_ADMIN", companyId));

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("COMPANY_ADMIN");
        assertThat(jwt.getClaimAsString("companyId")).isEqualTo(companyId.toString());
        assertThat(jwt.getAudience()).containsExactly("panel");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(8)));
    }

    @Test
    void superadminTokenHasNoCompanyId() {
        Jwt jwt = decoder.decode(jwtService.issue(UUID.randomUUID(), "SUPERADMIN", null));

        assertThat(jwt.hasClaim("companyId")).isFalse();
    }
}
