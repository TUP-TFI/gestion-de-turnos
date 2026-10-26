package com.grupo140.turnos.schedule;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Excepciones de calendario (US-05.6) de punta a punta, sobre la empresa del seed, que no tiene ninguna
 * cargada y usa la zona horaria por defecto (America/Argentina/Cordoba).
 *
 * <p>Cada test hace rollback ({@code @Transactional}): las excepciones guardadas acá no quedan en la base para
 * los demás tests.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ScheduleExceptionIT {

    private static final String EXCEPTIONS = "/api/company/schedule-exceptions";
    private static final String AFFECTED = EXCEPTIONS + "/affected-appointments";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String SUPERADMIN_EMAIL = "superadmin@example.com";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Argentina/Cordoba"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleExceptionRepository scheduleExceptionRepository;

    @Test
    void createsAClosedDay() throws Exception {
        mockMvc.perform(adminJson(post(EXCEPTIONS), closed(TODAY.plusDays(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.exceptionDate").value(TODAY.plusDays(5).toString()))
                .andExpect(jsonPath("$.isClosed").value(true))
                .andExpect(jsonPath("$.startTime").value(nullValue()))
                .andExpect(jsonPath("$.intervalMinutes").value(nullValue()))
                .andExpect(jsonPath("$.reason").value("Feriado"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createsADayWithSpecialHours() throws Exception {
        mockMvc.perform(adminJson(post(EXCEPTIONS), special(TODAY.plusDays(5), "10:00", "14:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.startTime", startsWith("10:00")))
                .andExpect(jsonPath("$.endTime", startsWith("14:00")))
                .andExpect(jsonPath("$.intervalMinutes").value(30))
                .andExpect(jsonPath("$.reason").value(nullValue()));
    }

    @Test
    void listStartsTodayAndIsSortedByDate() throws Exception {
        UUID companyId = create(closed(TODAY.plusDays(10)));
        create(closed(TODAY));
        // Una excepción pasada no se lista. Se guarda directo porque la API no deja cargar fechas pasadas.
        scheduleExceptionRepository.saveAndFlush(new ScheduleException(companyId, TODAY.minusDays(1)));

        mockMvc.perform(asAdmin(get(EXCEPTIONS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].exceptionDate").value(TODAY.toString()))
                .andExpect(
                        jsonPath("$[1].exceptionDate").value(TODAY.plusDays(10).toString()));
    }

    @Test
    void pastDateIsRejected() throws Exception {
        mockMvc.perform(adminJson(post(EXCEPTIONS), closed(TODAY.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_EXCEPTION"))
                .andExpect(jsonPath("$.message").value("La fecha no puede ser anterior a hoy"));
    }

    @Test
    void hoursThatDoNotMatchIsClosedAreRejected() throws Exception {
        String date = TODAY.plusDays(5).toString();
        List<String> invalidBodies = List.of(
                // Cerrado con horario.
                "{\"exceptionDate\": \"%s\", \"isClosed\": true, \"startTime\": \"10:00\"}".formatted(date),
                // Abierto sin intervalo.
                "{\"exceptionDate\": \"%s\", \"isClosed\": false, \"startTime\": \"10:00\", \"endTime\": \"14:00\"}"
                        .formatted(date),
                // Abierto con el fin antes del inicio.
                special(TODAY.plusDays(5), "14:00", "10:00"));

        for (String invalid : invalidBodies) {
            mockMvc.perform(adminJson(post(EXCEPTIONS), invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_EXCEPTION"));
        }
    }

    @Test
    void missingDateOrIsClosedIsAValidationError() throws Exception {
        List<String> invalidBodies =
                List.of("{\"isClosed\": true}", "{\"exceptionDate\": \"%s\"}".formatted(TODAY.plusDays(5)), "{}");

        for (String invalid : invalidBodies) {
            mockMvc.perform(adminJson(post(EXCEPTIONS), invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void repeatedDateIsRejected() throws Exception {
        create(closed(TODAY.plusDays(5)));

        mockMvc.perform(adminJson(post(EXCEPTIONS), special(TODAY.plusDays(5), "10:00", "14:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_EXCEPTION_DATE_TAKEN"));
    }

    @Test
    void updateKeepingTheSameDateChangesTheHours() throws Exception {
        String id = idOf(createAndReturn(closed(TODAY.plusDays(5))));

        mockMvc.perform(adminJson(put(EXCEPTIONS + "/" + id), special(TODAY.plusDays(5), "10:00", "14:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.startTime", startsWith("10:00")));

        // Y de vuelta a cerrado: se borran las horas.
        mockMvc.perform(adminJson(put(EXCEPTIONS + "/" + id), closed(TODAY.plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isClosed").value(true))
                .andExpect(jsonPath("$.startTime").value(nullValue()));
    }

    @Test
    void updateToTheDateOfAnotherExceptionIsRejected() throws Exception {
        create(closed(TODAY.plusDays(5)));
        String id = idOf(createAndReturn(closed(TODAY.plusDays(6))));

        mockMvc.perform(adminJson(put(EXCEPTIONS + "/" + id), closed(TODAY.plusDays(5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_EXCEPTION_DATE_TAKEN"));
    }

    @Test
    void deleteRemovesTheException() throws Exception {
        String id = idOf(createAndReturn(closed(TODAY.plusDays(5))));

        mockMvc.perform(asAdmin(delete(EXCEPTIONS + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asAdmin(get(EXCEPTIONS))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(asAdmin(delete(EXCEPTIONS + "/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_EXCEPTION_NOT_FOUND"));
    }

    @Test
    void exceptionsOfAnotherCompanyAreNotVisible() throws Exception {
        ScheduleException other =
                scheduleExceptionRepository.saveAndFlush(new ScheduleException(anotherCompany(), TODAY.plusDays(5)));
        String otherPath = EXCEPTIONS + "/" + other.getId();

        mockMvc.perform(asAdmin(get(EXCEPTIONS))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(adminJson(put(otherPath), closed(TODAY.plusDays(6)))).andExpect(status().isNotFound());
        mockMvc.perform(asAdmin(delete(otherPath))).andExpect(status().isNotFound());
        // La misma fecha que usa la otra empresa está libre para esta.
        mockMvc.perform(adminJson(post(EXCEPTIONS), closed(TODAY.plusDays(5)))).andExpect(status().isCreated());
    }

    @Test
    void superadminAndAnonymousCannotEnter() throws Exception {
        mockMvc.perform(get(EXCEPTIONS).header("Authorization", "Bearer " + tokenOf(SUPERADMIN_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(EXCEPTIONS)).andExpect(status().isUnauthorized());
    }

    @Test
    void affectedCountsZeroWithoutAppointmentsAndSavesNothing() throws Exception {
        mockMvc.perform(adminJson(post(AFFECTED), closed(TODAY.plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // Contar no guarda la excepción propuesta.
        mockMvc.perform(asAdmin(get(EXCEPTIONS))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void affectedRejectsAnInvalidException() throws Exception {
        mockMvc.perform(adminJson(post(AFFECTED), closed(TODAY.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_EXCEPTION"));
    }

    private static String closed(LocalDate date) {
        return "{\"exceptionDate\": \"%s\", \"isClosed\": true, \"reason\": \"Feriado\"}".formatted(date);
    }

    private static String special(LocalDate date, String start, String end) {
        return ("{\"exceptionDate\": \"%s\", \"isClosed\": false, \"startTime\": \"%s\", \"endTime\": \"%s\","
                        + " \"intervalMinutes\": 30}")
                .formatted(date, start, end);
    }

    // Crea la excepción y devuelve el id de la empresa dueña.
    private UUID create(String body) throws Exception {
        return UUID.fromString(JsonPath.read(createAndReturn(body), "$.companyId"));
    }

    private String createAndReturn(String body) throws Exception {
        return mockMvc.perform(adminJson(post(EXCEPTIONS), body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String idOf(String body) {
        return JsonPath.read(body, "$.id");
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
