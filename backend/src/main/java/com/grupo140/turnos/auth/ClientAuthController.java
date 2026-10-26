package com.grupo140.turnos.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y login del cliente final, desde la página pública de una empresa. Van bajo
 * {@code /api/public/**}: todavía no hay sesión.
 */
@RestController
@RequestMapping("/api/public/companies/{slug}")
public class ClientAuthController {

    private final AuthService authService;

    public ClientAuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Registro del cliente; queda con la sesión iniciada (US-03.5). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@PathVariable String slug, @Valid @RequestBody ClientRegisterRequest request) {
        return authService.registerClient(slug, request);
    }

    /** Login del cliente (US-03.6). */
    @PostMapping("/login")
    public LoginResponse login(@PathVariable String slug, @Valid @RequestBody LoginRequest request) {
        return authService.loginClient(slug, request);
    }
}
