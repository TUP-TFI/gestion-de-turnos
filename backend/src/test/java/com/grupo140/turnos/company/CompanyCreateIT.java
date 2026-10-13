package com.grupo140.turnos.company;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de empresa (US-04.1) de punta a punta contra PostgreSQL real, con los usuarios del seed de dev.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): las empresas creadas acá no quedan en la base
 * para los demás tests.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyCreateIT {

    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";
    private static final String SEED_ADMIN_EMAIL = "admin@example.com";
    private static final String NEW_ADMIN_EMAIL = "sol@example.com";
    private static final String TIMEZONE = "America/Argentina/Cordoba";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsActiveCompanyWithGeneratedSlugAndPendingAdmin() throws Exception {
        mockMvc.perform(createCompany(SUPERADMIN_EMAIL, "Estética Sol", TIMEZONE, NEW_ADMIN_EMAIL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.slug").value("estetica-sol"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timezone").value(TIMEZONE))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        // El admin existe pero todavía no definió su contraseña.
        mockMvc.perform(login(NEW_ADMIN_EMAIL, DEV_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVATED"));
    }

    @Test
    void existingCompanyNameGetsSlugWithSuffix() throws Exception {
        // "Barbería Central" es la empresa del seed: su slug ya está ocupado.
        mockMvc.perform(createCompany(SUPERADMIN_EMAIL, "Barbería Central", TIMEZONE, NEW_ADMIN_EMAIL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("barberia-central-2"));
    }

    @Test
    void existingAdminEmailReturns409() throws Exception {
        mockMvc.perform(createCompany(SUPERADMIN_EMAIL, "Estética Sol", TIMEZONE, SEED_ADMIN_EMAIL))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void unknownTimezoneReturnsValidationError() throws Exception {
        mockMvc.perform(createCompany(SUPERADMIN_EMAIL, "Estética Sol", "America/Cualquiera", NEW_ADMIN_EMAIL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void companyAdminTokenReturns403() throws Exception {
        mockMvc.perform(createCompany(SEED_ADMIN_EMAIL, "Estética Sol", TIMEZONE, NEW_ADMIN_EMAIL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpServletRequestBuilder createCompany(
            String loggedInEmail, String companyName, String timezone, String adminEmail) throws Exception {
        String body = """
                {
                  "company": {"name": "%s", "category": "BEAUTY_CENTER", "timezone": "%s"},
                  "admin": {"firstName": "Sol", "lastName": "Díaz", "email": "%s", "phone": "+5493510000010"}
                }
                """.formatted(companyName, timezone, adminEmail);
        return post("/api/superadmin/companies")
                .header("Authorization", "Bearer " + tokenOf(loggedInEmail))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
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
