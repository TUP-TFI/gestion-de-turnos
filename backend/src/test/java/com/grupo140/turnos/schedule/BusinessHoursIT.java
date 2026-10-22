package com.grupo140.turnos.schedule;

import static com.grupo140.turnos.seed.DevDataSeeder.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
 * Horario semanal (US-05.3) y aviso de turnos fuera de rango (US-05.4) de punta a punta, sobre la empresa
 * del seed: lunes a viernes de 09:00 a 13:00 y de 16:00 a 20:00, sábado de 09:00 a 13:00, domingo cerrado.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los horarios guardados acá no quedan en la base para
 * los demás tests.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BusinessHoursIT {

    private static final String HOURS = "/api/company/business-hours";
    private static final String OUT_OF_RANGE = HOURS + "/out-of-range-appointments";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessHoursRepository businessHoursRepository;

    @Test
    void listReturnsSeedHoursSortedByDayAndStart() throws Exception {
        mockMvc.perform(asAdmin(get(HOURS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime", startsWith("09:00")))
                .andExpect(jsonPath("$[1].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[1].startTime", startsWith("16:00")))
                .andExpect(jsonPath("$[10].dayOfWeek").value("SATURDAY"));
    }

    @Test
    void putReplacesTheWholeWeek() throws Exception {
        String week = hours(slot("TUESDAY", "10:00", "14:00"), slot("MONDAY", "08:00", "12:00"));

        mockMvc.perform(adminJson(put(HOURS), week))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[1].dayOfWeek").value("TUESDAY"));

        // Las franjas del seed ya no están.
        mockMvc.perform(asAdmin(get(HOURS)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].startTime", startsWith("08:00")));
    }

    @Test
    void contiguousSlotsAreAccepted() throws Exception {
        String week = hours(slot("MONDAY", "09:00", "13:00"), slot("MONDAY", "13:00", "17:00"));

        mockMvc.perform(adminJson(put(HOURS), week))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void overlappingSlotsAreRejectedAndKeepPreviousHours() throws Exception {
        String week = hours(slot("MONDAY", "09:00", "13:00"), slot("MONDAY", "12:00", "17:00"));

        mockMvc.perform(adminJson(put(HOURS), week))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"))
                .andExpect(jsonPath("$.message").value("Las franjas del lunes se superponen"));

        mockMvc.perform(asAdmin(get(HOURS))).andExpect(jsonPath("$", hasSize(11)));
    }

    @Test
    void endBeforeStartIsRejected() throws Exception {
        mockMvc.perform(adminJson(put(HOURS), hours(slot("TUESDAY", "13:00", "09:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));
    }

    @Test
    void missingOrZeroIntervalIsAValidationError() throws Exception {
        List<String> invalidBodies = List.of(
                "{\"hours\": [{\"dayOfWeek\": \"MONDAY\", \"startTime\": \"09:00\", \"endTime\": \"13:00\"}]}",
                "{\"hours\": [{\"dayOfWeek\": \"MONDAY\", \"startTime\": \"09:00\", \"endTime\": \"13:00\","
                        + " \"intervalMinutes\": 0}]}",
                "{}");

        for (String invalid : invalidBodies) {
            mockMvc.perform(adminJson(put(HOURS), invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void emptyWeekClosesEveryDay() throws Exception {
        mockMvc.perform(adminJson(put(HOURS), hours())).andExpect(status().isOk());

        mockMvc.perform(asAdmin(get(HOURS))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putDoesNotTouchHoursOfAnotherCompany() throws Exception {
        UUID otherCompanyId = anotherCompany();
        businessHoursRepository.save(
                new BusinessHours(otherCompanyId, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0), 60));

        mockMvc.perform(adminJson(put(HOURS), hours())).andExpect(status().isOk());

        assertThat(businessHoursRepository.findAllByCompanyId(otherCompanyId)).hasSize(1);
    }

    @Test
    void superadminAndAnonymousCannotEnter() throws Exception {
        mockMvc.perform(get(HOURS).header("Authorization", "Bearer " + tokenOf(SUPERADMIN_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(HOURS)).andExpect(status().isUnauthorized());
    }

    @Test
    void outOfRangeCountsZeroWithoutAppointmentsAndSavesNothing() throws Exception {
        mockMvc.perform(adminJson(post(OUT_OF_RANGE), hours(slot("MONDAY", "10:00", "11:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // Contar no guarda el horario propuesto.
        mockMvc.perform(asAdmin(get(HOURS))).andExpect(jsonPath("$", hasSize(11)));
    }

    @Test
    void outOfRangeRejectsInvalidHours() throws Exception {
        String week = hours(slot("MONDAY", "09:00", "13:00"), slot("MONDAY", "12:00", "17:00"));

        mockMvc.perform(adminJson(post(OUT_OF_RANGE), week))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));
    }

    private static String hours(String... slots) {
        return "{\"hours\": [" + String.join(", ", slots) + "]}";
    }

    private static String slot(String day, String start, String end) {
        return "{\"dayOfWeek\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"intervalMinutes\": 30}"
                .formatted(day, start, end);
    }

    // Crea una segunda empresa como superadmin y devuelve su id.
    private UUID anotherCompany() throws Exception {
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
        return UUID.fromString(JsonPath.read(body, "$.id"));
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
