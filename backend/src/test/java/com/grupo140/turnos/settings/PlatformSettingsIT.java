package com.grupo140.turnos.settings;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Parámetros globales (US-04.9) de punta a punta. Los valores por defecto son los que inserta la migración V1.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): la fila vuelve a sus valores por defecto.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PlatformSettingsIT {

    private static final String SETTINGS = "/api/superadmin/settings";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getReturnsDefaultValues() throws Exception {
        mockMvc.perform(asUser("superadmin@example.com", get(SETTINGS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minBookingNoticeMinutes").value(30))
                .andExpect(jsonPath("$.cancellationDeadlineHours").value(3))
                .andExpect(jsonPath("$.maxRescheduleCount").value(2))
                .andExpect(jsonPath("$.maxBookingAdvanceDays").value(90));
    }

    @Test
    void updateSavesNewValues() throws Exception {
        mockMvc.perform(update("""
                        {"minBookingNoticeMinutes": 60, "cancellationDeadlineHours": 24,
                         "maxRescheduleCount": 0, "maxBookingAdvanceDays": 30}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minBookingNoticeMinutes").value(60))
                .andExpect(jsonPath("$.maxRescheduleCount").value(0));

        // El cambio quedó guardado: la próxima lectura ya trae los valores nuevos.
        mockMvc.perform(asUser("superadmin@example.com", get(SETTINGS)))
                .andExpect(jsonPath("$.minBookingNoticeMinutes").value(60))
                .andExpect(jsonPath("$.cancellationDeadlineHours").value(24))
                .andExpect(jsonPath("$.maxRescheduleCount").value(0))
                .andExpect(jsonPath("$.maxBookingAdvanceDays").value(30));
    }

    @Test
    void zeroNoticeReturnsValidationError() throws Exception {
        mockMvc.perform(update("""
                        {"minBookingNoticeMinutes": 0, "cancellationDeadlineHours": 3,
                         "maxRescheduleCount": 2, "maxBookingAdvanceDays": 90}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingFieldReturnsValidationError() throws Exception {
        mockMvc.perform(update("""
                        {"minBookingNoticeMinutes": 30, "cancellationDeadlineHours": 3, "maxRescheduleCount": 2}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void companyAdminCannotReadSettings() throws Exception {
        mockMvc.perform(asUser("admin@example.com", get(SETTINGS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpServletRequestBuilder update(String body) throws Exception {
        return asUser("superadmin@example.com", put(SETTINGS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
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
