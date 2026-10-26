package com.grupo140.turnos.user;

import com.grupo140.turnos.common.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
    public Optional<UserCredentials> findPanelUserByEmail(String email) {
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

    /**
     * Admin de la empresa, o vacío si no tiene (no debería pasar: el alta siempre crea uno). Si hubiera más
     * de uno, devuelve el más antiguo.
     */
    @Transactional(readOnly = true)
    public Optional<CompanyAdmin> findCompanyAdmin(UUID companyId) {
        return userRepository
                .findFirstByCompanyIdAndRoleOrderByCreatedAtAsc(companyId, UserRole.COMPANY_ADMIN)
                .map(UserService::toCompanyAdmin);
    }

    /**
     * Igual que {@link #findCompanyAdmin}, pero bloquea la fila del admin hasta el fin de la transacción.
     * Hay que llamarlo dentro de una transacción de escritura.
     */
    @Transactional
    public Optional<CompanyAdmin> lockCompanyAdmin(UUID companyId) {
        return userRepository
                .findFirstForUpdateByCompanyIdAndRoleOrderByCreatedAtAsc(companyId, UserRole.COMPANY_ADMIN)
                .map(UserService::toCompanyAdmin);
    }

    /**
     * Indica si el email ya está en uso en esa empresa, con cualquier rol: el índice único
     * {@code ux_user_email_per_company} no distingue roles.
     */
    @Transactional(readOnly = true)
    public boolean isEmailTakenInCompany(UUID companyId, String email) {
        return userRepository.findByEmailAndCompanyId(email, companyId).isPresent();
    }

    /**
     * Crea un cliente ya activo, con la contraseña que eligió (US-03.5).
     *
     * @param passwordHash hash BCrypt de la contraseña.
     * @throws ApiException {@code EMAIL_ALREADY_IN_USE} (409) si otro registro simultáneo ganó el email.
     */
    @Transactional
    public UserCredentials createClient(
            UUID companyId, String firstName, String lastName, String email, String phone, String passwordHash) {
        User client = new User(firstName, lastName, email, phone, UserRole.CLIENT, companyId, UserStatus.ACTIVE);
        client.setPassword(passwordHash);
        try {
            return toCredentials(userRepository.saveAndFlush(client));
        } catch (DataIntegrityViolationException ex) {
            // Dos registros a la vez con el mismo email: el chequeo previo no alcanza, lo frena el índice único.
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE", "Ya existe una cuenta con ese email");
        }
    }

    /** Cliente de la empresa por email. Un admin con ese email en la misma empresa no cuenta. */
    @Transactional(readOnly = true)
    public Optional<UserCredentials> findClientCredentials(UUID companyId, String email) {
        return userRepository
                .findByEmailAndCompanyId(email, companyId)
                .filter(user -> user.getRole() == UserRole.CLIENT)
                .map(UserService::toCredentials);
    }

    /**
     * Teléfono de un cliente de la empresa, para avisarle de un cambio en su turno (US-04.4). Vacío si el
     * usuario no existe o es de otra empresa.
     */
    @Transactional(readOnly = true)
    public Optional<String> findPhone(UUID userId, UUID companyId) {
        return userRepository.findByIdAndCompanyId(userId, companyId).map(User::getPhone);
    }

    private static CompanyAdmin toCompanyAdmin(User admin) {
        return new CompanyAdmin(admin.getId(), admin.getEmail(), admin.getPhone(), admin.getStatus());
    }

    private static UserCredentials toCredentials(User user) {
        return new UserCredentials(
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
