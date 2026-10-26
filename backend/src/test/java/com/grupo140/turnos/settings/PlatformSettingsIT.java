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

    // Issue #47
    @Test
    void negativeRescheduleCountReturnsValidationError() throws Exception {
        mockMvc.perform(update(body(30, 3, -1, 90)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get(SETTINGS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(put(SETTINGS).contentType(MediaType.APPLICATION_JSON).content(body(30, 3, 2, 90)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTypesReturnValidationError() throws Exception {
        mockMvc.perform(update("""
                        {"minBookingNoticeMinutes": "abc", "cancellationDeadlineHours": 3,
                         "maxRescheduleCount": 2, "maxBookingAdvanceDays": 90}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(update("""
                        {"minBookingNoticeMinutes": 30.5, "cancellationDeadlineHours": 3,
                         "maxRescheduleCount": 2, "maxBookingAdvanceDays": 90}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void companyAdminCannotUpdateSettings() throws Exception {
        mockMvc.perform(asUser("admin@example.com", put(SETTINGS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(30, 3, 2, 90)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // Issue #45: un valor por encima de cada tope responde 400, y el tope mismo se acepta.
    @Test
    void valueAboveMaximumReturnsValidationError() throws Exception {
        for (String invalid :
                new String[] {body(1441, 3, 2, 365), body(30, 169, 2, 90), body(30, 3, 11, 90), body(30, 3, 2, 366)}) {
            mockMvc.perform(update(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(update(body(1440, 168, 10, 365))).andExpect(status().isOk());
    }

    // Issue #46, caso límite: 1 día de máxima son 1440 minutos, igual al tope de la mínima, y pasa. Con los
    // topes actuales la ventana vacía no se puede armar por HTTP; el rechazo se prueba en PlatformSettingsServiceTest.
    @Test
    void minimumNoticeEqualToMaximumIsAccepted() throws Exception {
        mockMvc.perform(update(body(1440, 3, 2, 1))).andExpect(status().isOk());
    }

    private static String body(int notice, int deadline, int reschedules, int advanceDays) {
        return """
                {"minBookingNoticeMinutes": %d, "cancellationDeadlineHours": %d,
                 "maxRescheduleCount": %d, "maxBookingAdvanceDays": %d}
                """.formatted(notice, deadline, reschedules, advanceDays);
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
