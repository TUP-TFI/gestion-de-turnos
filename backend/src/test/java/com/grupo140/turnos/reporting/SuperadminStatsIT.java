package com.grupo140.turnos.reporting;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Dashboard del superadmin (US-04.6) de punta a punta.
 *
 * <p>Compara antes y después de crear una empresa, así el test no depende de cuántas empresas trae el seed.
 * Cada test hace rollback ({@code @Transactional}): la empresa creada no queda para los demás tests.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SuperadminStatsIT {

    private static final String STATS = "/api/superadmin/stats";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheThreeIndicators() throws Exception {
        mockMvc.perform(asUser("superadmin@example.com", get(STATS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCompanies").isNumber())
                .andExpect(jsonPath("$.inactiveCompanies").isNumber())
                .andExpect(jsonPath("$.totalAppointments").isNumber());
    }

    @Test
    void newCompanyIsCountedAsActive() throws Exception {
        long before = stat("activeCompanies");

        createCompany();

        assertThat(stat("activeCompanies")).isEqualTo(before + 1);
    }

    @Test
    void deactivatedCompanyMovesFromActiveToInactive() throws Exception {
        String companyId = createCompany();
        long activeBefore = stat("activeCompanies");
        long inactiveBefore = stat("inactiveCompanies");

        mockMvc.perform(asUser(
                        "superadmin@example.com", post("/api/superadmin/companies/" + companyId + "/deactivate")))
                .andExpect(status().isOk());

        assertThat(stat("activeCompanies")).isEqualTo(activeBefore - 1);
        assertThat(stat("inactiveCompanies")).isEqualTo(inactiveBefore + 1);
    }

    @Test
    void companyAdminCannotSeeStats() throws Exception {
        mockMvc.perform(asUser("admin@example.com", get(STATS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // Crea una empresa con el alta y devuelve su id.
    private String createCompany() throws Exception {
        String body = mockMvc.perform(asUser("superadmin@example.com", post("/api/superadmin/companies"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company": {"name": "Estudio Stats", "category": "OTHER", "timezone": "America/Cordoba"},
                                  "admin": {"firstName": "Ana", "lastName": "Paz", "email": "stats@example.com", "phone": "+5493510000030"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // Uno de los tres indicadores del dashboard, por su nombre en el JSON.
    private long stat(String field) throws Exception {
        String body = mockMvc.perform(asUser("superadmin@example.com", get(STATS)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number count = JsonPath.read(body, "$." + field);
        return count.longValue();
    }

    private MockHttpServletRequestBuilder asUser(String email, MockHttpServletRequestBuilder request) throws Exception {
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
