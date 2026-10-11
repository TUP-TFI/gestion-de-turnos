package com.grupo140.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

    private static final String PLAIN_TOKEN = "plain-token";
    private static final String PASSWORD = "NuevaClave123";

    private final Instant now = Instant.parse("2026-10-10T12:00:00Z");
    private final TokenGenerator tokenGenerator = new TokenGenerator();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private UserService userService;

    private ActivationService activationService;

    @BeforeEach
    void setUp() {
        activationService = new ActivationService(
                userTokenRepository, tokenGenerator, userService, passwordEncoder, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void validTokenActivatesUserAndMarksTokenUsed() {
        UserToken token = token(TokenType.ACTIVATION, now.plus(1, ChronoUnit.DAYS));
        givenToken(token);
        when(userService.activate(eq(userId), anyString())).thenReturn(true);

        activationService.activate(new ActivationRequest(PLAIN_TOKEN, PASSWORD));

        verify(userService).activate(eq(userId), argThat(hash -> passwordEncoder.matches(PASSWORD, hash)));
        assertThat(token.getUsedAt()).isEqualTo(now);
    }

    @Test
    void unknownTokenIsInvalid() {
        when(userTokenRepository.findForUpdateByTokenHash(tokenGenerator.hash(PLAIN_TOKEN)))
                .thenReturn(Optional.empty());

        assertError("INVALID_ACTIVATION_TOKEN");
        verify(userService, never()).activate(any(), any());
    }

    @Test
    void passwordResetTokenIsInvalid() {
        givenToken(token(TokenType.PASSWORD_RESET, now.plus(1, ChronoUnit.HOURS)));

        assertError("INVALID_ACTIVATION_TOKEN");
        verify(userService, never()).activate(any(), any());
    }

    @Test
    void usedTokenIsInvalid() {
        UserToken token = token(TokenType.ACTIVATION, now.plus(1, ChronoUnit.DAYS));
        token.markUsed(now.minus(1, ChronoUnit.HOURS));
        givenToken(token);

        assertError("INVALID_ACTIVATION_TOKEN");
        verify(userService, never()).activate(any(), any());
    }

    @Test
    void expiredTokenIsRejected() {
        UserToken token = token(TokenType.ACTIVATION, now.minus(1, ChronoUnit.MINUTES));
        givenToken(token);

        assertError("ACTIVATION_TOKEN_EXPIRED");
        verify(userService, never()).activate(any(), any());
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void userNotPendingIsInvalidAndTokenStaysUnused() {
        UserToken token = token(TokenType.ACTIVATION, now.plus(1, ChronoUnit.DAYS));
        givenToken(token);
        when(userService.activate(eq(userId), anyString())).thenReturn(false);

        assertError("INVALID_ACTIVATION_TOKEN");
        assertThat(token.getUsedAt()).isNull();
    }

    private UserToken token(TokenType type, Instant expiresAt) {
        return new UserToken(userId, tokenGenerator.hash(PLAIN_TOKEN), type, expiresAt);
    }

    private void givenToken(UserToken token) {
        when(userTokenRepository.findForUpdateByTokenHash(tokenGenerator.hash(PLAIN_TOKEN)))
                .thenReturn(Optional.of(token));
    }

    private void assertError(String code) {
        assertThatThrownBy(() -> activationService.activate(new ActivationRequest(PLAIN_TOKEN, PASSWORD)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo(code);
                });
    }
}
