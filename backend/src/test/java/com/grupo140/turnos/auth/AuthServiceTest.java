package com.grupo140.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grupo140.turnos.common.error.ApiException;
import com.grupo140.turnos.company.CompanyService;
import com.grupo140.turnos.user.UserCredentials;
import com.grupo140.turnos.user.UserRole;
import com.grupo140.turnos.user.UserService;
import com.grupo140.turnos.user.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "Turnos.dev.2026";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserService userService;

    @Mock
    private CompanyService companyService;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, companyService, passwordEncoder, jwtService);
    }

    @Test
    void superadminLoginSucceedsWithoutCompanyId() {
        UserCredentials user = user(UserRole.SUPERADMIN, null, UserStatus.ACTIVE, PASSWORD);
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.issue(user.id(), "SUPERADMIN", null, JwtService.PANEL_AUDIENCE))
                .thenReturn("token");

        LoginResponse response = authService.loginPanel(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.token()).isEqualTo("token");
        assertThat(response.user().role()).isEqualTo("SUPERADMIN");
        assertThat(response.user().companyId()).isNull();
        verify(companyService, never()).isActive(any());
    }

    @Test
    void companyAdminLoginSucceedsWithCompanyId() {
        UUID companyId = UUID.randomUUID();
        UserCredentials user = user(UserRole.COMPANY_ADMIN, companyId, UserStatus.ACTIVE, PASSWORD);
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyService.isActive(companyId)).thenReturn(true);
        when(jwtService.issue(user.id(), "COMPANY_ADMIN", companyId, JwtService.PANEL_AUDIENCE))
                .thenReturn("token");

        LoginResponse response = authService.loginPanel(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.user().companyId()).isEqualTo(companyId);
        assertThat(response.user().email()).isEqualTo(EMAIL);
    }

    @Test
    void unknownEmailIsInvalidCredentials() {
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.empty());

        assertError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        verify(jwtService, never()).issue(any(), any(), any(), any());
    }

    @Test
    void wrongPasswordIsInvalidCredentials() {
        UserCredentials user = user(UserRole.SUPERADMIN, null, UserStatus.ACTIVE, "otra-contraseña");
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }

    @Test
    void pendingActivationAccountIsRejected() {
        UserCredentials user = user(UserRole.COMPANY_ADMIN, UUID.randomUUID(), UserStatus.PENDING_ACTIVATION, null);
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertError(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVATED");
    }

    @Test
    void inactiveUserIsInvalidCredentials() {
        UserCredentials user = user(UserRole.COMPANY_ADMIN, UUID.randomUUID(), UserStatus.INACTIVE, PASSWORD);
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }

    @Test
    void inactiveCompanyIsRejected() {
        UUID companyId = UUID.randomUUID();
        UserCredentials user = user(UserRole.COMPANY_ADMIN, companyId, UserStatus.ACTIVE, PASSWORD);
        when(userService.findPanelUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(companyService.isActive(eq(companyId))).thenReturn(false);

        assertError(HttpStatus.FORBIDDEN, "COMPANY_INACTIVE");
        verify(jwtService, never()).issue(any(), any(), any(), any());
    }

    private void assertError(HttpStatus status, String code) {
        assertThatThrownBy(() -> authService.loginPanel(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(status);
                    assertThat(ex.getCode()).isEqualTo(code);
                });
    }

    private UserCredentials user(UserRole role, UUID companyId, UserStatus status, String rawPassword) {
        String hash = rawPassword == null ? null : passwordEncoder.encode(rawPassword);
        return new UserCredentials(UUID.randomUUID(), "Carlos", "Gómez", EMAIL, role, companyId, status, hash);
    }
}
