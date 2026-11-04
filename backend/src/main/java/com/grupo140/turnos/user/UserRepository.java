package com.grupo140.turnos.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a {@link User}. Extiende {@link Repository} y no {@code JpaRepository} a propósito: así no
 * hereda {@code findById(id)} ni {@code findAll()}, y la única forma de leer un usuario de empresa es
 * pasando su {@code companyId} (regla de aislamiento multi-tenant del README, US-03.9).
 *
 * <p>Las búsquedas por email comparan con {@code lower(...)} para usar los índices únicos
 * {@code ux_user_email_per_company} y {@code ux_superadmin_email}, definidos sobre {@code lower(email)}.
 */
public interface UserRepository extends Repository<User, UUID> {

    User save(User user);

    // Igual que save, pero escribe en el acto: el registro de clientes necesita que el índice único salte acá y no
    // al cerrar la transacción.
    User saveAndFlush(User user);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

    // Admin de una empresa. Cada empresa tiene uno solo: lo crea el alta (US-04.1). Si hubiera más de uno
    // por un dato inconsistente, se toma el más antiguo en lugar de fallar con un 500.
    Optional<User> findFirstByCompanyIdAndRoleOrderByCreatedAtAsc(UUID companyId, UserRole role);

    // Igual que la anterior, pero bloquea la fila hasta el fin de la transacción: el reenvío del link
    // (US-04.8) la usa para que dos reenvíos simultáneos no dejen dos tokens vigentes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findFirstForUpdateByCompanyIdAndRoleOrderByCreatedAtAsc(UUID companyId, UserRole role);

    // Única búsqueda por id sin companyId: la usa la activación (US-03.4), donde el token del link ya
    // prueba la identidad del usuario. Filtrar por estado acota el acceso a cuentas pendientes.
    Optional<User> findByIdAndStatus(UUID id, UserStatus status);

    @Query("select u from User u where lower(u.email) = lower(:email) and u.companyId = :companyId")
    Optional<User> findByEmailAndCompanyId(@Param("email") String email, @Param("companyId") UUID companyId);

    // Superadmin: es el único rol sin empresa (CHECK chk_user_company_by_role).
    @Query("select u from User u where lower(u.email) = lower(:email) and u.companyId is null")
    Optional<User> findSuperadminByEmail(@Param("email") String email);

    // Login del panel: SUPERADMIN y COMPANY_ADMIN. Devuelve lista para que el login detecte duplicados.
    @Query("""
            select u from User u
            where lower(u.email) = lower(:email)
              and u.role in (com.grupo140.turnos.user.UserRole.SUPERADMIN,
                             com.grupo140.turnos.user.UserRole.COMPANY_ADMIN)
            """)
    List<User> findPanelUsersByEmail(@Param("email") String email);

    // Clientes de la empresa (US-09.4 y US-09.7). pattern es un patrón de like en minúsculas: '%' trae todos. El id
    // desempata el orden, para que un cliente no se repita ni falte entre dos páginas.
    @Query("""
            select u from User u
            where u.companyId = :companyId
              and u.role = com.grupo140.turnos.user.UserRole.CLIENT
              and (lower(concat(u.firstName, ' ', u.lastName)) like :pattern escape '\\'
                   or lower(u.email) like :pattern escape '\\'
                   or u.phone like :pattern escape '\\')
            order by u.lastName, u.firstName, u.id
            """)
    Page<User> searchClients(@Param("companyId") UUID companyId, @Param("pattern") String pattern, Pageable pageable);
}
