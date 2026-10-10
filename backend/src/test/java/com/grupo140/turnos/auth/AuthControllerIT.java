package com.grupo140.turnos.auth;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Login y seguridad de punta a punta contra PostgreSQL real, con los usuarios del seed de dev.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void superadminLoginReturnsNullCompanyId() throws Exception {
        mockMvc.perform(login("superadmin@example.com", DEV_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("SUPERADMIN"))
                .andExpect(jsonPath("$.user.companyId").value((Object) null));
    }

    @Test
    void companyAdminLoginReturnsCompanyId() throws Exception {
        mockMvc.perform(login("admin@example.com", DEV_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.user.companyId").isNotEmpty());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(login("admin@example.com", "incorrecta"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void pendingActivationAccountReturns403() throws Exception {
        mockMvc.perform(login("admin.pendiente@example.com", DEV_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVATED"));
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithTokenReturnsSession() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.companyId").isNotEmpty());
    }

    @Test
    void adminTokenOnSuperadminRouteReturns403() throws Exception {
        mockMvc.perform(get("/api/superadmin/x").header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void superadminTokenPassesSecurityAndUnknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/superadmin/x").header("Authorization", "Bearer " + tokenOf("superadmin@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void preflightFromFrontendReturnsCorsHeader() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(login(email, DEV_PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }
}
