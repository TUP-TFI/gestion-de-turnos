package com.grupo140.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo140.turnos.TestcontainersConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purga de tokens (US-11.3) con el admin del seed, llamando al método directo, sin esperar al scheduler.
 *
 * <p>El borrado masivo no pasa por las entidades cargadas, así que el resultado se lee de la base con
 * {@link JdbcTemplate}. Cada test hace rollback ({@code @Transactional}): otros tests dependen de los tokens del seed.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class PurgeTokensIT {

    @Autowired
    private ActivationService activationService;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject("select id from app_user where email = 'admin@example.com'", UUID.class);
    }

    @Test
    void purgeDeletesExpiredAndUsedTokensAndKeepsValidOnes() {
        Instant now = Instant.now();
        UUID expired = save(now.minus(Duration.ofDays(1)), null);
        UUID used = save(now.plus(Duration.ofDays(1)), now);
        UUID valid = save(now.plus(Duration.ofDays(1)), null);

        activationService.purgeDeadTokens();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(used)).isFalse();
        assertThat(exists(valid)).isTrue();
    }

    @Test
    void purgeDoesNotDeleteTheUser() {
        Instant now = Instant.now();
        save(now.minus(Duration.ofDays(1)), null);

        activationService.purgeDeadTokens();

        assertThat(jdbcTemplate.queryForObject("select count(*) from app_user where id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    // Con un instante fijo se prueba el corte exacto: el token que vence justo ahora ya no sirve.
    @Test
    void cutoffIsInclusiveOnExpiresAt() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID expiresNow = save(now, null);
        UUID expiresLater = save(now.plus(Duration.ofSeconds(1)), null);

        userTokenRepository.deleteExpiredOrUsed(now);

        assertThat(exists(expiresNow)).isFalse();
        assertThat(exists(expiresLater)).isTrue();
    }

    private UUID save(Instant expiresAt, Instant usedAt) {
        UserToken token = new UserToken(userId, UUID.randomUUID().toString(), TokenType.ACTIVATION, expiresAt);
        if (usedAt != null) {
            token.markUsed(usedAt);
        }
        return userTokenRepository.save(token).getId();
    }

    private boolean exists(UUID tokenId) {
        Integer count =
                jdbcTemplate.queryForObject("select count(*) from user_token where id = ?", Integer.class, tokenId);
        return count == 1;
    }
}
