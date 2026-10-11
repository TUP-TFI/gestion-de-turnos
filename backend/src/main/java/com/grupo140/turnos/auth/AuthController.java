package com.grupo140.turnos.auth;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticación del panel.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loginPanel(request);
    }

    /** Sesión actual. El frontend lo usa para revalidar el token al recargar. */
    @GetMapping("/me")
    public SessionUser me(SessionUser session) {
        return session;
    }
}
