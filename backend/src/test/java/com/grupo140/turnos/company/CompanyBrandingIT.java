package com.grupo140.turnos.company;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
 * Marca de la empresa (US-05.7) de punta a punta, con el admin de Barbería Central del seed. La otra empresa
 * del seed, Estética Pendiente, sirve para comprobar que el cambio de una no toca a la otra.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): la marca del seed vuelve a quedar como estaba.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyBrandingIT {

    private static final String BRANDING = "/api/company/branding";
    private static final String COMPANIES = "/api/superadmin/companies";
    private static final String LOGO = "https://example.com/logo.png";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saveAndReadBack() throws Exception {
        mockMvc.perform(update(body(LOGO, "#1A2B3C")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("barberia-central"))
                .andExpect(jsonPath("$.logoUrl").value(LOGO))
                .andExpect(jsonPath("$.primaryColor").value("#1A2B3C"));

        mockMvc.perform(asAdmin(get(BRANDING)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("barberia-central"))
                .andExpect(jsonPath("$.logoUrl").value(LOGO))
                .andExpect(jsonPath("$.primaryColor").value("#1A2B3C"));
    }

    @Test
    void nullClearsBothValues() throws Exception {
        mockMvc.perform(update(body(LOGO, "#1A2B3C"))).andExpect(status().isOk());

        mockMvc.perform(update("{\"logoUrl\": null, \"primaryColor\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.primaryColor").doesNotExist());
        // Un campo ausente cuenta como null: el PUT reemplaza los dos valores.
        mockMvc.perform(update(body(LOGO, "#1A2B3C"))).andExpect(status().isOk());
        mockMvc.perform(update("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").doesNotExist())
                .andExpect(jsonPath("$.primaryColor").doesNotExist());
    }

    @Test
    void slugInBodyIsIgnored() throws Exception {
        // El slug es de solo lectura: el PUT no lo cambia aunque llegue en el body.
        mockMvc.perform(update("{\"slug\": \"otro-slug\", \"logoUrl\": null, \"primaryColor\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("barberia-central"));
    }

    @Test
    void invalidColorReturns400() throws Exception {
        for (String color : List.of("red", "#12345", "#1234567", "#GGGGGG", "1A2B3C", "")) {
            mockMvc.perform(update(body(LOGO, color)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void logoOver500CharactersReturns400() throws Exception {
        mockMvc.perform(update(body("https://example.com/" + "a".repeat(481), "#1A2B3C")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        // 500 caracteres justos entran: es el largo de la columna.
        mockMvc.perform(update(body("https://example.com/" + "a".repeat(480), "#1A2B3C")))
                .andExpect(status().isOk());
    }

    @Test
    void superadminCannotUseCompanyBranding() throws Exception {
        mockMvc.perform(as("superadmin@example.com", get(BRANDING)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get(BRANDING))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void changeIsVisibleToSuperadminAndDoesNotTouchOtherCompany() throws Exception {
        String otherBefore = companyBranding("estetica-pendiente");

        mockMvc.perform(update(body(LOGO, "#1A2B3C"))).andExpect(status().isOk());

        // El superadmin ve lo que guardó el admin, en el detalle de la empresa.
        String id = companyIdBySlug("barberia-central");
        mockMvc.perform(as("superadmin@example.com", get(COMPANIES + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(LOGO))
                .andExpect(jsonPath("$.primaryColor").value("#1A2B3C"));
        assertThat(companyBranding("estetica-pendiente")).isEqualTo(otherBefore);
    }

    private static String body(String logoUrl, String primaryColor) {
        return "{\"logoUrl\": \"%s\", \"primaryColor\": \"%s\"}".formatted(logoUrl, primaryColor);
    }

    private MockHttpServletRequestBuilder update(String body) throws Exception {
        return asAdmin(put(BRANDING)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as("admin@example.com", request);
    }

    private MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenOf(email));
    }

    // Los ids de las empresas del seed se generan al cargarlas, así que se buscan en el listado.
    private String companyIdBySlug(String slug) throws Exception {
        String body = mockMvc.perform(as("superadmin@example.com", get(COMPANIES)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].id");
        return ids.getFirst();
    }

    // Logo y color de una empresa, como los ve el superadmin en el listado.
    private String companyBranding(String slug) throws Exception {
        String body = mockMvc.perform(as("superadmin@example.com", get(COMPANIES)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> logos = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].logoUrl");
        List<String> colors = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].primaryColor");
        return logos + " " + colors;
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
