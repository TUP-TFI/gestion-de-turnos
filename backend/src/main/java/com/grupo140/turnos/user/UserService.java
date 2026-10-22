package com.grupo140.turnos.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API de {@code user} para el resto de los features. {@code auth} entra por acá y no por
 * {@link UserRepository}, así la entidad {@link User} no sale del paquete.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca un usuario del panel por email. El email de {@code COMPANY_ADMIN} es único globalmente, así
     * que a lo sumo hay uno; si aparece más de uno es un dato inconsistente y se devuelve vacío.
     */
    @Transactional(readOnly = true)
    public Optional<PanelUserCredentials> findPanelUserByEmail(String email) {
        List<User> users = userRepository.findPanelUsersByEmail(email);
        if (users.size() > 1) {
            log.warn("Hay {} usuarios del panel con el mismo email: se rechaza el login", users.size());
            return Optional.empty();
        }
        return users.stream().findFirst().map(UserService::toCredentials);
    }

    /**
     * Define la contraseña de una cuenta {@code PENDING_ACTIVATION} y la deja {@code ACTIVE} (US-03.4).
     *
     * @return {@code false} si el usuario no existe o ya no está pendiente de activación.
     */
    @Transactional
    public boolean activate(UUID userId, String passwordHash) {
        return userRepository
                .findByIdAndStatus(userId, UserStatus.PENDING_ACTIVATION)
                .map(user -> {
                    user.setPassword(passwordHash);
                    user.setStatus(UserStatus.ACTIVE);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Indica si el email ya pertenece a un usuario del panel ({@code SUPERADMIN} o {@code COMPANY_ADMIN}
     * de cualquier empresa). El alta de empresa lo usa para no repetir el email de un admin (US-04.1).
     */
    @Transactional(readOnly = true)
    public boolean isPanelEmailTaken(String email) {
        return !userRepository.findPanelUsersByEmail(email).isEmpty();
    }

    /**
     * Crea el admin de una empresa recién dada de alta (US-04.1). Queda {@code PENDING_ACTIVATION} y sin
     * contraseña: la define él mismo con el link de activación.
     *
     * @return el id del usuario creado.
     */
    @Transactional
    public UUID createPendingAdmin(UUID companyId, String firstName, String lastName, String email, String phone) {
        User admin = new User(
                firstName, lastName, email, phone, UserRole.COMPANY_ADMIN, companyId, UserStatus.PENDING_ACTIVATION);
        return userRepository.save(admin).getId();
    }

    /** Admin de la empresa, o vacío si no tiene (no debería pasar: el alta siempre crea uno). */
    @Transactional(readOnly = true)
    public Optional<CompanyAdmin> findCompanyAdmin(UUID companyId) {
        return userRepository
                .findByCompanyIdAndRole(companyId, UserRole.COMPANY_ADMIN)
                .map(admin -> new CompanyAdmin(admin.getId(), admin.getEmail(), admin.getPhone(), admin.getStatus()));
    }

    private static PanelUserCredentials toCredentials(User user) {
        return new PanelUserCredentials(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId(),
                user.getStatus(),
                user.getPassword());
    }
}
