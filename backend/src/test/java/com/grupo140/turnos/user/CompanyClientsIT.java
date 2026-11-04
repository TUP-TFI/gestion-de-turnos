package com.grupo140.turnos.user;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.hamcrest.Matchers.contains;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado y búsqueda de clientes del admin (US-09.4 y US-09.7) de punta a punta, con los clientes del seed de
 * Barbería Central: Lucía Pérez y Martín Sosa. Cada test hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyClientsIT {

    private static final String CLIENTS = "/api/company/clients";
    private static final String LUCIA = "cliente1@example.com";
    private static final String MARTIN = "cliente2@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsCompanyClientsByLastNameWithoutTheAdmin() throws Exception {
        listAsAdmin("", "0", "20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", contains(LUCIA, MARTIN)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void defaultsAreFirstPageOfTwentyWithoutFilter() throws Exception {
        mockMvc.perform(get(CLIENTS).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void responseCarriesOnlyContactData() throws Exception {
        listAsAdmin("lucía", "0", "20")
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].firstName").value("Lucía"))
                .andExpect(jsonPath("$.content[0].lastName").value("Pérez"))
                .andExpect(jsonPath("$.content[0].email").value(LUCIA))
                .andExpect(jsonPath("$.content[0].phone").value("+5493510000004"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].role").doesNotExist())
                .andExpect(jsonPath("$.content[0].status").doesNotExist())
                .andExpect(jsonPath("$.content[0].companyId").doesNotExist());
    }

    @Test
    void searchMatchesNameEmailAndPhoneIgnoringCase() throws Exception {
        assertOnlyMatch("MARTÍN", MARTIN);
        assertOnlyMatch("  sosa  ", MARTIN);
        assertOnlyMatch("lucía pérez", LUCIA);
        assertOnlyMatch("CLIENTE2@", MARTIN);
        assertOnlyMatch("0000004", LUCIA);
    }

    @Test
    void searchWithoutMatchesReturnsEmptyPage() throws Exception {
        listAsAdmin("nadie", "0", "20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void likeWildcardsAreSearchedAsText() throws Exception {
        listAsAdmin("%", "0", "20").andExpect(jsonPath("$.totalElements").value(0));
        listAsAdmin("cliente_", "0", "20").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pagesSplitResultsAndKeepTotals() throws Exception {
        listAsAdmin("", "0", "1")
                .andExpect(jsonPath("$.content[*].email", contains(LUCIA)))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        listAsAdmin("", "1", "1")
                .andExpect(jsonPath("$.content[*].email", contains(MARTIN)))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void clientOfAnotherCompanyIsNotListed() throws Exception {
        registerClient("estetica-pendiente", "forastera@example.com");

        listAsAdmin("forastera", "0", "20")
                .andExpect(jsonPath("$.totalElements").value(0));
        listAsAdmin("", "0", "20").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void invalidPageOrSizeReturns400() throws Exception {
        String[][] invalid = {{"-1", "20"}, {"0", "0"}, {"0", "101"}, {"abc", "20"}};

        for (String[] params : invalid) {
            listAsAdmin("", params[0], params[1])
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void clientCannotListClients() throws Exception {
        String body = mockMvc.perform(post("/api/public/companies/barberia-central/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(LUCIA, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(get(CLIENTS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get(CLIENTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void assertOnlyMatch(String query, String email) throws Exception {
        listAsAdmin(query, "0", "20").andExpect(jsonPath("$.content[*].email", contains(email)));
    }

    private ResultActions listAsAdmin(String query, String page, String size) throws Exception {
        return mockMvc.perform(get(CLIENTS)
                .header("Authorization", "Bearer " + adminToken())
                .param("q", query)
                .param("page", page)
                .param("size", size));
    }

    private void registerClient(String slug, String email) throws Exception {
        String json = "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"%s\","
                + "\"phone\":\"+5493510000099\",\"password\":\"Clave.nueva.2026\"}";
        mockMvc.perform(post("/api/public/companies/" + slug + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String adminToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"%s\"}".formatted(DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
