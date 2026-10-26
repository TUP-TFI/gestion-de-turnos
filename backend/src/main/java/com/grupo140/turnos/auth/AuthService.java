package com.grupo140.turnos.auth;

import com.grupo140.turnos.auth.LoginResponse.LoggedUser;
import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.user.UserCredentials;
import com.grupo140.turnos.user.UserRole;
import com.grupo140.turnos.user.UserService;
import com.grupo140.turnos.user.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login del panel (US-03.1 y US-03.2), y registro y login del cliente final (US-03.5 y US-03.6).
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
        Optional<UserCredentials> found = userService.findPanelUserByEmail(request.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        UserCredentials user = found.get();

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
        String token = jwtService.issue(user.id(), role, user.companyId(), JwtService.PANEL_AUDIENCE);
        return toResponse(user, token);
    }

    /**
     * Registra a un cliente en la empresa de la página pública y lo deja con la sesión iniciada (US-03.5).
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404), {@code COMPANY_INACTIVE} (403) o
     *     {@code EMAIL_ALREADY_IN_USE} (409).
     */
    @Transactional
    public LoginResponse registerClient(String slug, ClientRegisterRequest request) {
        UUID companyId = companyService.requireActiveIdBySlug(slug);
        if (userService.isEmailTakenInCompany(companyId, request.email())) {
            throw emailAlreadyInUse();
        }

        UserCredentials client = userService.createClient(
                companyId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                passwordEncoder.encode(request.password()));
        String token = jwtService.issue(client.id(), client.role().name(), companyId, JwtService.CLIENT_AUDIENCE);
        return toResponse(client, token);
    }

    /**
     * Login del cliente en la empresa de la página pública (US-03.6). Un admin de esa empresa no entra por acá.
     *
     * @throws ApiException {@code COMPANY_NOT_FOUND} (404), {@code COMPANY_INACTIVE} (403) o
     *     {@code INVALID_CREDENTIALS} (401).
     */
    @Transactional(readOnly = true)
    public LoginResponse loginClient(String slug, LoginRequest request) {
        UUID companyId = companyService.requireActiveIdBySlug(slug);

        Optional<UserCredentials> found = userService.findClientCredentials(companyId, request.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        UserCredentials client = found.get();

        if (client.status() != UserStatus.ACTIVE
                || client.passwordHash() == null
                || !passwordEncoder.matches(request.password(), client.passwordHash())) {
            throw invalidCredentials();
        }
        String token = jwtService.issue(client.id(), client.role().name(), companyId, JwtService.CLIENT_AUDIENCE);
        return toResponse(client, token);
    }

    private static LoginResponse toResponse(UserCredentials user, String token) {
        return new LoginResponse(
                token,
                new LoggedUser(
                        user.id(),
                        user.firstName(),
                        user.lastName(),
                        user.email(),
                        user.role().name(),
                        user.companyId()));
    }

    private static ApiException emailAlreadyInUse() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_IN_USE", "Ya existe una cuenta con ese email");
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contraseña incorrectos");
    }
}
