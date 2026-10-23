package com.grupo140.turnos.company;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
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
 * Desactivar y reactivar una empresa (US-04.4 y US-04.5) de punta a punta, sobre Barbería Central del seed. La
 * cancelación de turnos se prueba aparte, en {@code CompanyDeactivationCascadeIT}.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): la empresa vuelve a quedar activa.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyStatusIT {

    private static final String COMPANIES = "/api/superadmin/companies";
    private static final String SEED_SLUG = "barberia-central";
    private static final String SEED_ADMIN_EMAIL = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deactivateLeavesCompanyInactiveAndRepeatingIsHarmless() throws Exception {
        String id = companyIdBySlug(SEED_SLUG);

        mockMvc.perform(action(id, "deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.slug").value(SEED_SLUG));

        // Repetir no da error: sigue inactiva.
        mockMvc.perform(action(id, "deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void reactivateLeavesCompanyActiveAndRepeatingIsHarmless() throws Exception {
        String id = companyIdBySlug(SEED_SLUG);
        mockMvc.perform(action(id, "deactivate")).andExpect(status().isOk());

        mockMvc.perform(action(id, "reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(action(id, "reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminCannotLogInWhileCompanyIsInactive() throws Exception {
        String id = companyIdBySlug(SEED_SLUG);
        mockMvc.perform(action(id, "deactivate")).andExpect(status().isOk());

        mockMvc.perform(login(SEED_ADMIN_EMAIL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));

        // Al reactivar vuelve a entrar con su contraseña de siempre.
        mockMvc.perform(action(id, "reactivate")).andExpect(status().isOk());
        mockMvc.perform(login(SEED_ADMIN_EMAIL)).andExpect(status().isOk());
    }

    @Test
    void unknownCompanyReturns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();

        mockMvc.perform(action(unknownId, "deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
        mockMvc.perform(action(unknownId, "reactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void companyAdminCannotDeactivate() throws Exception {
        String id = companyIdBySlug(SEED_SLUG);

        mockMvc.perform(post(COMPANIES + "/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + tokenOf(SEED_ADMIN_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpServletRequestBuilder action(String companyId, String action) throws Exception {
        return post(COMPANIES + "/" + companyId + "/" + action)
                .header("Authorization", "Bearer " + tokenOf("superadmin@example.com"));
    }

    // Los ids de las empresas del seed se generan al cargarlas, así que se buscan en el listado.
    private String companyIdBySlug(String slug) throws Exception {
        String body = mockMvc.perform(
                        get(COMPANIES).header("Authorization", "Bearer " + tokenOf("superadmin@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].id");
        return ids.getFirst();
    }

    private static MockHttpServletRequestBuilder login(String email) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD));
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(login(email))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
