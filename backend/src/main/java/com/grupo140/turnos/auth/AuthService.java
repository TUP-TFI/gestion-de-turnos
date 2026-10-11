package com.grupo140.turnos.auth;

import com.grupo140.turnos.auth.LoginResponse.LoggedUser;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.user.PanelUserCredentials;
import com.grupo140.turnos.user.UserRole;
import com.grupo140.turnos.user.UserService;
import com.grupo140.turnos.user.UserStatus;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login del panel (US-03.1 y US-03.2): superadmin y admin de empresa.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Hash de relleno: con un email inexistente se compara igual, para que el tiempo de respuesta
    // no delate si la cuenta existe.
    private final String dummyHash;

    public AuthService(
            UserService userService,
            CompanyService companyService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userService = userService;
        this.companyService = companyService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * Valida las credenciales y emite el JWT.
     *
     * @throws ApiException {@code INVALID_CREDENTIALS} (401), {@code ACCOUNT_NOT_ACTIVATED} (403) o
     *     {@code COMPANY_INACTIVE} (403).
     */
    @Transactional(readOnly = true)
    public LoginResponse loginPanel(LoginRequest request) {
        Optional<PanelUserCredentials> found = userService.findPanelUserByEmail(request.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        PanelUserCredentials user = found.get();

        // Va antes del chequeo de contraseña: es nula hasta que se activa la cuenta.
        if (user.status() == UserStatus.PENDING_ACTIVATION) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVATED", "Tu cuenta todavía no está activada");
        }
        if (user.status() == UserStatus.INACTIVE
                || user.passwordHash() == null
                || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }
        if (user.role() == UserRole.COMPANY_ADMIN && !companyService.isActive(user.companyId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMPANY_INACTIVE", "La empresa está desactivada");
        }

        String role = user.role().name();
        String token = jwtService.issue(user.id(), role, user.companyId());
        return new LoginResponse(
                token,
                new LoggedUser(user.id(), user.firstName(), user.lastName(), user.email(), role, user.companyId()));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contraseña incorrectos");
    }
}
