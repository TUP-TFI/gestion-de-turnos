package com.grupo140.turnos.auth;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación del panel.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ActivationService activationService;

    public AuthController(AuthService authService, ActivationService activationService) {
        this.authService = authService;
        this.activationService = activationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loginPanel(request);
    }

    /** Activación de la cuenta del admin con el token del link (US-03.4). */
    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@Valid @RequestBody ActivationRequest request) {
        activationService.activate(request);
    }

    /** Sesión actual. El frontend lo usa para revalidar el token al recargar. */
    @GetMapping("/me")
    public SessionUser me(SessionUser session) {
        return session;
    }
}
