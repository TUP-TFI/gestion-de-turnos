package com.grupo140.turnos.company;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_ACTIVATION_TOKEN;
import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
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
 * Reenvío del link de activación (US-04.8) de punta a punta, con las empresas del seed: Estética Pendiente
 * tiene su admin pendiente y Barbería Central lo tiene activo.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): el token del seed vuelve a quedar sin usar, porque
 * otros tests, como {@code AccountActivationIT}, lo necesitan.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ResendActivationIT {

    private static final String COMPANIES = "/api/superadmin/companies";
    private static final String PENDING_SLUG = "estetica-pendiente";
    private static final String ACTIVE_SLUG = "barberia-central";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resendForPendingAdminReturns204AndInvalidatesOldLink() throws Exception {
        mockMvc.perform(resend(companyIdBySlug(PENDING_SLUG))).andExpect(status().isNoContent());

        // El link anterior (el token del seed) deja de servir.
        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"password\":\"NuevaClave123\"}".formatted(DEV_ACTIVATION_TOKEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
    }

    // Decisión de la issue #42: con la empresa desactivada el reenvío se permite. El admin puede definir su
    // contraseña, pero el login sigue bloqueado hasta que la empresa se reactive.
    @Test
    void resendWorksForInactiveCompany() throws Exception {
        String companyId = companyIdBySlug(PENDING_SLUG);
        mockMvc.perform(asSuperadmin(post(COMPANIES + "/" + companyId + "/deactivate")))
                .andExpect(status().isOk());

        mockMvc.perform(resend(companyId)).andExpect(status().isNoContent());
    }

    @Test
    void activeAdminReturns409() throws Exception {
        mockMvc.perform(resend(companyIdBySlug(ACTIVE_SLUG)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_ALREADY_ACTIVE"));
    }

    @Test
    void unknownCompanyReturns404() throws Exception {
        mockMvc.perform(resend(UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder resend(String companyId) throws Exception {
        return asSuperadmin(post(COMPANIES + "/" + companyId + "/resend-activation"));
    }

    // Los ids de las empresas del seed se generan al cargarlas, así que se buscan en el listado.
    private String companyIdBySlug(String slug) throws Exception {
        String body = mockMvc.perform(asSuperadmin(get(COMPANIES)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder asSuperadmin(MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenOf("superadmin@example.com"));
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
