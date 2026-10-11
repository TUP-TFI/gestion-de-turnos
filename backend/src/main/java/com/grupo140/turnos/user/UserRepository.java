package com.grupo140.turnos.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);

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
}
