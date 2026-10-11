package com.grupo140.turnos.user;

import java.util.List;
import java.util.Optional;
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
