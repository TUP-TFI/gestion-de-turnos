package com.grupo140.turnos.auth;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link UserToken}. El token entrante se busca siempre por su hash (ver {@link TokenGenerator}):
 * el valor en claro nunca se persiste.
 */
public interface UserTokenRepository extends Repository<UserToken, UUID> {

    UserToken save(UserToken token);

    Optional<UserToken> findByTokenHash(String tokenHash);

    // Tokens todavía sin usar de un usuario: el reenvío los invalida antes de crear uno nuevo (US-04.8).
    List<UserToken> findByUserIdAndTypeAndUsedAtIsNull(UUID userId, TokenType type);

    // Canje del token (activación): bloquea la fila hasta el fin de la transacción.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserToken> findForUpdateByTokenHash(String tokenHash);
}
