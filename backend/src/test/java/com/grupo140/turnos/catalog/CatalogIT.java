package com.grupo140.turnos.catalog;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
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
 * Catálogo de servicios (US-05.1 y US-05.2) de punta a punta, sobre la empresa del seed.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los servicios creados o editados acá no quedan
 * en la base para los demás tests.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogIT {

    private static final String SERVICES = "/api/company/services";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void listIncludesSeedServices() throws Exception {
        mockMvc.perform(asAdmin(get(SERVICES)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("Corte de pelo", "Corte y barba")));
    }

    @Test
    void createReturnsActiveService() throws Exception {
        mockMvc.perform(adminJson(post(SERVICES), body("Barba", 3500.50, 20)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Barba"))
                .andExpect(jsonPath("$.price").value(3500.50))
                .andExpect(jsonPath("$.durationMinutes").value(20))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void blankDescriptionIsSavedAsNull() throws Exception {
        String json = "{\"name\": \"Barba\", \"description\": \"   \", \"price\": 3500, \"durationMinutes\": 20}";

        mockMvc.perform(adminJson(post(SERVICES), json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void createWithInvalidDataReturnsValidationError() throws Exception {
        List<String> invalidBodies =
                List.of("{\"price\": 100, \"durationMinutes\": 30}", body("Barba", -1, 30), body("Barba", 100, 0));

        for (String invalid : invalidBodies) {
            mockMvc.perform(adminJson(post(SERVICES), invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void updateChangesNamePriceAndDuration() throws Exception {
        String id = seedServiceId();

        mockMvc.perform(adminJson(put(SERVICES + "/" + id), body("Corte clásico", 5000, 45)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Corte clásico"))
                .andExpect(jsonPath("$.price").value(5000))
                .andExpect(jsonPath("$.durationMinutes").value(45));

        // El cambio quedó guardado.
        mockMvc.perform(asAdmin(get(SERVICES + "/" + id)))
                .andExpect(jsonPath("$.name").value("Corte clásico"));
    }

    @Test
    void deactivateKeepsServiceInAdminList() throws Exception {
        String id = seedServiceId();

        mockMvc.perform(asAdmin(post(SERVICES + "/" + id + "/deactivate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Repetir la desactivación no falla.
        mockMvc.perform(asAdmin(post(SERVICES + "/" + id + "/deactivate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(asAdmin(get(SERVICES)))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].status").value("INACTIVE"));

        mockMvc.perform(asAdmin(post(SERVICES + "/" + id + "/activate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void unknownServiceReturns404() throws Exception {
        mockMvc.perform(asAdmin(get(SERVICES + "/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void serviceOfAnotherCompanyReturns404() throws Exception {
        String otherServiceId = serviceOfAnotherCompany();

        mockMvc.perform(asAdmin(get(SERVICES + "/" + otherServiceId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        mockMvc.perform(adminJson(put(SERVICES + "/" + otherServiceId), body("Robado", 1, 10)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        mockMvc.perform(asAdmin(post(SERVICES + "/" + otherServiceId + "/deactivate")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        mockMvc.perform(asAdmin(get(SERVICES + "/" + otherServiceId + "/future-appointments")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void superadminAndAnonymousCannotEnter() throws Exception {
        mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + tokenOf(SUPERADMIN_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SERVICES)).andExpect(status().isUnauthorized());
    }

    @Test
    void futureAppointmentsCountsZeroForOwnService() throws Exception {
        mockMvc.perform(asAdmin(get(SERVICES + "/" + seedServiceId() + "/future-appointments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    private static String body(String name, Number price, int durationMinutes) {
        return "{\"name\": \"%s\", \"price\": %s, \"durationMinutes\": %d}".formatted(name, price, durationMinutes);
    }

    private String seedServiceId() throws Exception {
        String body = mockMvc.perform(asAdmin(get(SERVICES)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.name == 'Corte de pelo')].id");
        return ids.getFirst();
    }

    // Crea una segunda empresa (como superadmin) y le guarda un servicio directo en el repositorio.
    private String serviceOfAnotherCompany() throws Exception {
        String companyJson = """
                {"company": {"name": "Estética Sol", "category": "BEAUTY_CENTER",
                             "timezone": "America/Argentina/Cordoba"},
                 "admin": {"firstName": "Sol", "lastName": "Díaz", "email": "sol@example.com",
                           "phone": "+5493510000099"}}
                """;
        String body = mockMvc.perform(post("/api/superadmin/companies")
                        .header("Authorization", "Bearer " + tokenOf(SUPERADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID companyId = UUID.fromString(JsonPath.read(body, "$.id"));
        Service service =
                serviceRepository.saveAndFlush(new Service(companyId, "Limpieza facial", new BigDecimal("8000"), 60));
        return service.getId().toString();
    }

    private MockHttpServletRequestBuilder adminJson(MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return asAdmin(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenOf(ADMIN_EMAIL));
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
