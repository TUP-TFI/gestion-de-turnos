package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
 * Horarios disponibles de la página pública (US-06.2) de punta a punta, sin login, con el horario del seed de
 * Barbería Central: lunes a viernes de 9 a 13 y de 16 a 20, cada 30 minutos, y domingo cerrado. El cálculo en sí
 * lo cubre {@code AvailabilityServiceTest}; acá se prueba el endpoint.
 *
 * <p>Cada test hace rollback ({@code @Transactional}): los cambios no pasan al test siguiente.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicAvailabilityIT {

    private static final String PUBLIC = "/api/public/companies/";
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Cordoba");
    // Al menos dos días adelante, para que la anticipación mínima no saque horarios.
    private static final LocalDate MONDAY =
            LocalDate.now(ZONE).plusDays(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void weekdayReturnsFreeStartsWithoutLogin() throws Exception {
        UUID serviceId = seedServiceId("Corte de pelo");

        // 30 minutos: 8 inicios a la mañana (9:00 a 12:30) y 8 a la tarde (16:00 a 19:30), en UTC.
        mockMvc.perform(availability("barberia-central", serviceId, MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(MONDAY.toString()))
                .andExpect(jsonPath("$.starts.length()").value(16))
                .andExpect(jsonPath("$.starts[0]").value(at(MONDAY, 9, 0).toString()))
                .andExpect(jsonPath("$.starts[15]").value(at(MONDAY, 19, 30).toString()));
    }

    @Test
    void closedDayReturnsEmptyList() throws Exception {
        LocalDate sunday = MONDAY.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

        mockMvc.perform(availability("barberia-central", seedServiceId("Corte de pelo"), sunday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starts.length()").value(0));
    }

    @Test
    void bookedAppointmentRemovesItsStarts() throws Exception {
        UUID serviceId = seedServiceId("Corte y barba");
        saveAppointment(seedServiceId("Corte de pelo"), at(MONDAY, 10, 0));

        // 60 minutos: sin turnos serían 7 + 7. El de 10:00 a 10:30 deja afuera las 9:30 y las 10:00.
        mockMvc.perform(availability("barberia-central", serviceId, MONDAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starts.length()").value(12))
                .andExpect(jsonPath("$.starts[0]").value(at(MONDAY, 9, 0).toString()))
                .andExpect(jsonPath("$.starts[1]").value(at(MONDAY, 10, 30).toString()));
    }

    @Test
    void inactiveServiceReturns404() throws Exception {
        UUID serviceId = seedServiceId("Corte de pelo");
        mockMvc.perform(as("admin@example.com", post("/api/company/services/" + serviceId + "/deactivate")))
                .andExpect(status().isOk());

        mockMvc.perform(availability("barberia-central", serviceId, MONDAY.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void serviceOfAnotherCompanyReturns404() throws Exception {
        mockMvc.perform(availability("estetica-pendiente", seedServiceId("Corte de pelo"), MONDAY.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void inactiveCompanyReturns403() throws Exception {
        UUID serviceId = seedServiceId("Corte de pelo");
        String companyId = companyIdBySlug("barberia-central");
        mockMvc.perform(as("superadmin@example.com", post("/api/superadmin/companies/" + companyId + "/deactivate")))
                .andExpect(status().isOk());

        mockMvc.perform(availability("barberia-central", serviceId, MONDAY.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_INACTIVE"));
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mockMvc.perform(availability("no-existe", UUID.randomUUID(), MONDAY.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void invalidOrMissingDateReturns400() throws Exception {
        UUID serviceId = seedServiceId("Corte de pelo");

        mockMvc.perform(availability("barberia-central", serviceId, "02/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(PUBLIC + "barberia-central/services/" + serviceId + "/availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private static MockHttpServletRequestBuilder availability(String slug, UUID serviceId, String date) {
        return get(PUBLIC + slug + "/services/" + serviceId + "/availability").param("date", date);
    }

    // Hora local de la empresa en una fecha, pasada a instante.
    private static Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant();
    }

    // Turno manual de 30 minutos (sin cuenta), para no depender de los clientes del seed.
    private void saveAppointment(UUID serviceId, Instant start) throws Exception {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(UUID.fromString(companyIdBySlug("barberia-central")));
        appointment.setServiceId(serviceId);
        appointment.setManualClientName("Cliente de prueba");
        appointment.setManualClientPhone("+5493510000123");
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(30);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(30)));
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointmentRepository.save(appointment);
    }

    // Un servicio de Barbería Central, buscado por nombre en el listado del admin.
    private UUID seedServiceId(String name) throws Exception {
        String body = mockMvc.perform(as("admin@example.com", get("/api/company/services")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.name == '" + name + "')].id");
        return UUID.fromString(ids.getFirst());
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
