package com.grupo140.turnos.auth;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_ACTIVATION_TOKEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activación de cuenta (US-03.4) de punta a punta con el admin pendiente del seed de dev.
 *
 * <p>Cada test hace rollback ({@code @Transactional}) para que el admin siga pendiente: otros tests,
 * como {@link AuthControllerIT}, dependen de ese estado.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountActivationIT {

    private static final String PENDING_EMAIL = "admin.pendiente@example.com";
    private static final String NEW_PASSWORD = "NuevaClave123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activationReturns204AndAllowsLogin() throws Exception {
        mockMvc.perform(activate(DEV_ACTIVATION_TOKEN, NEW_PASSWORD)).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(PENDING_EMAIL, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("COMPANY_ADMIN"));
    }

    @Test
    void reusedTokenReturnsInvalidToken() throws Exception {
        mockMvc.perform(activate(DEV_ACTIVATION_TOKEN, NEW_PASSWORD)).andExpect(status().isNoContent());

        mockMvc.perform(activate(DEV_ACTIVATION_TOKEN, "OtraClave456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
    }

    @Test
    void unknownTokenReturnsInvalidToken() throws Exception {
        mockMvc.perform(activate("token-inventado", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
    }

    @Test
    void shortPasswordReturnsValidationError() throws Exception {
        mockMvc.perform(activate(DEV_ACTIVATION_TOKEN, "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static MockHttpServletRequestBuilder activate(String token, String password) {
        return post("/api/auth/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, password));
    }
}
