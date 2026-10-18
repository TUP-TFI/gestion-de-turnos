package com.grupo140.turnos.auth;

import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activación de la cuenta del admin de empresa (US-03.4): canjea el token del link por una contraseña y
 * deja al usuario {@code ACTIVE}. No emite JWT: después de activar, el usuario entra por el login.
 */
@Service
public class ActivationService {

    // Vigencia del link de activación (US-04.1).
    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(48);

    private final UserTokenRepository userTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ActivationService(
            UserTokenRepository userTokenRepository,
            TokenGenerator tokenGenerator,
            UserService userService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.userTokenRepository = userTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Crea el token de activación de un admin recién creado (US-04.1). En la base queda solo el hash.
     *
     * @return el valor en claro, que va en el link de activación.
     */
    @Transactional
    public String createActivationToken(UUID userId) {
        TokenGenerator.GeneratedToken token = tokenGenerator.generate();
        userTokenRepository.save(new UserToken(
                userId, token.hash(), TokenType.ACTIVATION, clock.instant().plus(ACTIVATION_TOKEN_TTL)));
        return token.plainValue();
    }

    /**
     * Reenvío del link de activación (US-04.8): marca como usados los tokens anteriores del usuario, así el
     * link viejo deja de servir, y crea uno nuevo de 48 h.
     *
     * @return el valor en claro del token nuevo, que va en el link.
     */
    @Transactional
    public String renewActivationToken(UUID userId) {
        Instant now = clock.instant();
        userTokenRepository
                .findByUserIdAndTypeAndUsedAtIsNull(userId, TokenType.ACTIVATION)
                .forEach(oldToken -> oldToken.markUsed(now));
        return createActivationToken(userId);
    }

    /**
     * Valida el token, define la contraseña y sella el token como usado.
     *
     * @throws ApiException {@code INVALID_ACTIVATION_TOKEN} (400) o {@code ACTIVATION_TOKEN_EXPIRED} (400).
     */
    @Transactional
    public void activate(ActivationRequest request) {
        // Con lock: dos requests simultáneos con el mismo token no pueden activar dos veces.
        UserToken token = userTokenRepository
                .findForUpdateByTokenHash(tokenGenerator.hash(request.token()))
                .filter(found -> found.getType() == TokenType.ACTIVATION && found.getUsedAt() == null)
                .orElseThrow(ActivationService::invalidToken);

        if (!token.getExpiresAt().isAfter(clock.instant())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACTIVATION_TOKEN_EXPIRED",
                    "El link de activación venció. Pedí uno nuevo al administrador de la plataforma");
        }
        if (!userService.activate(token.getUserId(), passwordEncoder.encode(request.password()))) {
            throw invalidToken();
        }
        token.markUsed(clock.instant());
    }

    private static ApiException invalidToken() {
        return new ApiException(
                HttpStatus.BAD_REQUEST, "INVALID_ACTIVATION_TOKEN", "El link de activación no es válido o ya se usó");
    }
}
