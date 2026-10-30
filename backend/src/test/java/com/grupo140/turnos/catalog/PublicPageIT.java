package com.grupo140.turnos.catalog;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.company.Company;
import com.grupo140.turnos.company.CompanyRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
 * Página pública (US-06.1) de punta a punta, sin login, con las dos empresas del seed: Barbería Central, con dos
 * servicios activos, y Estética Pendiente, sin servicios.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los cambios no pasan al test siguiente.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicPageIT {

    private static final String PUBLIC_PAGE = "/api/public/companies/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void activeCompanyIsVisibleWithoutLogin() throws Exception {
        mockMvc.perform(get(PUBLIC_PAGE + "barberia-central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.name").value("Barbería Central"))
                .andExpect(jsonPath("$.company.slug").value("barberia-central"))
                .andExpect(jsonPath("$.company.timezone").value("America/Argentina/Cordoba"))
                .andExpect(jsonPath("$.company.primaryColor").value("#1F2937"))
                .andExpect(jsonPath("$.company.status").value("ACTIVE"))
                .andExpect(jsonPath("$.company.id").doesNotExist())
                // Ordenados por nombre, con los datos que muestra la página y nada más.
                .andExpect(jsonPath("$.services.length()").value(2))
                .andExpect(jsonPath("$.services[0].name").value("Corte de pelo"))
                .andExpect(jsonPath("$.services[0].price").value(8000))
                .andExpect(jsonPath("$.services[0].durationMinutes").value(30))
                .andExpect(jsonPath("$.services[0].id").exists())
                .andExpect(jsonPath("$.services[0].companyId").doesNotExist())
                .andExpect(jsonPath("$.services[0].status").doesNotExist())
                .andExpect(jsonPath("$.services[1].name").value("Corte y barba"));
    }

    @Test
    void inactiveServiceIsHidden() throws Exception {
        String id = serviceIdByName("Corte de pelo");
        mockMvc.perform(as("admin@example.com", post("/api/company/services/" + id + "/deactivate")))
                .andExpect(status().isOk());

        mockMvc.perform(get(PUBLIC_PAGE + "barberia-central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services.length()").value(1))
                .andExpect(jsonPath("$.services[0].name").value("Corte y barba"));
    }

    @Test
    void eachCompanyShowsOnlyItsOwnServices() throws Exception {
        UUID otherId = companyRepository
                .findBySlug("estetica-pendiente")
                .map(Company::getId)
                .orElseThrow();
        serviceRepository.saveAndFlush(new Service(otherId, "Limpieza facial", new BigDecimal("8000"), 60));

        mockMvc.perform(get(PUBLIC_PAGE + "estetica-pendiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services.length()").value(1))
                .andExpect(jsonPath("$.services[0].name").value("Limpieza facial"));
        mockMvc.perform(get(PUBLIC_PAGE + "barberia-central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services.length()").value(2));
    }

    @Test
    void inactiveCompanyReturnsStatusWithoutServices() throws Exception {
        String id = companyIdBySlug("barberia-central");
        mockMvc.perform(as("superadmin@example.com", post("/api/superadmin/companies/" + id + "/deactivate")))
                .andExpect(status().isOk());

        // No es un error: la página tiene que poder mostrar que el negocio no está disponible.
        mockMvc.perform(get(PUBLIC_PAGE + "barberia-central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.name").value("Barbería Central"))
                .andExpect(jsonPath("$.company.status").value("INACTIVE"))
                .andExpect(jsonPath("$.services.length()").value(0));
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mockMvc.perform(get(PUBLIC_PAGE + "no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    private String serviceIdByName(String name) throws Exception {
        String body = mockMvc.perform(get(PUBLIC_PAGE + "barberia-central"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$.services[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    // Los ids de las empresas del seed se generan al cargarlas, así que se buscan en el listado.
    private String companyIdBySlug(String slug) throws Exception {
        String body = mockMvc.perform(as("superadmin@example.com", get("/api/superadmin/companies")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenOf(email));
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
