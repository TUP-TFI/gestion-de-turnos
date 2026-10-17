package com.grupo140.turnos.company;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_PASSWORD;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Listado, detalle y edición de empresas (US-04.2 y US-04.3) de punta a punta, sobre la empresa del seed.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): la empresa del seed vuelve a quedar como estaba.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyEditIT {

    private static final String COMPANIES = "/api/superadmin/companies";
    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";
    private static final String SEED_SLUG = "barberia-central";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listIncludesSeedCompany() throws Exception {
        mockMvc.perform(asSuperadmin(get(COMPANIES)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", hasItem(SEED_SLUG)));
    }

    @Test
    void companyAdminCannotList() throws Exception {
        mockMvc.perform(get(COMPANIES).header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getReturnsCompany() throws Exception {
        mockMvc.perform(asSuperadmin(get(COMPANIES + "/" + seedCompanyId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(SEED_SLUG))
                .andExpect(jsonPath("$.name").value("Barbería Central"));
    }

    @Test
    void getUnknownCompanyReturns404() throws Exception {
        mockMvc.perform(asSuperadmin(get(COMPANIES + "/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void getWithMalformedIdReturns404() throws Exception {
        // Un id que no es UUID no puede existir: 404, no 500.
        mockMvc.perform(asSuperadmin(get(COMPANIES + "/no-es-un-uuid")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateChangesDataButKeepsSlug() throws Exception {
        String id = seedCompanyId();

        mockMvc.perform(update(id, "Barbería Norte", "America/Cordoba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Barbería Norte"))
                .andExpect(jsonPath("$.category").value("HAIR_SALON"))
                .andExpect(jsonPath("$.timezone").value("America/Cordoba"))
                .andExpect(jsonPath("$.description").value("Nueva descripción"))
                .andExpect(jsonPath("$.slug").value(SEED_SLUG))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // El cambio quedó guardado.
        mockMvc.perform(asSuperadmin(get(COMPANIES + "/" + id)))
                .andExpect(jsonPath("$.name").value("Barbería Norte"));
    }

    @Test
    void updateWithUnknownTimezoneReturnsValidationError() throws Exception {
        mockMvc.perform(update(seedCompanyId(), "Barbería Norte", "America/Cualquiera"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateUnknownCompanyReturns404() throws Exception {
        mockMvc.perform(update(UUID.randomUUID().toString(), "Barbería Norte", "America/Cordoba"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder update(String id, String name, String timezone) throws Exception {
        String body = """
                {"name": "%s", "category": "HAIR_SALON", "timezone": "%s", "description": "Nueva descripción"}
                """.formatted(name, timezone);
        return asSuperadmin(put(COMPANIES + "/" + id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    // El id de la empresa del seed se genera al cargarla, así que se busca en el listado.
    private String seedCompanyId() throws Exception {
        String body = mockMvc.perform(asSuperadmin(get(COMPANIES)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.slug == '" + SEED_SLUG + "')].id");
        return ids.getFirst();
    }

    private MockHttpServletRequestBuilder asSuperadmin(MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenOf(SUPERADMIN_EMAIL));
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
