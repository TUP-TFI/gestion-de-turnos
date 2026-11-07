package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda y resumen del admin (US-09.1 y US-09.2) sobre Barbería Central del seed. Los turnos se guardan directo con el
 * repositorio, porque hacen falta fechas, estados y orígenes que la reserva no permite. Cada test hace rollback
 * ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyAgendaIT {

    private static final String APPOINTMENTS = "/api/company/appointments";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private UUID companyId;
    private UUID serviceId;
    private UUID luciaId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        String services = mockMvc.perform(get("/api/company/services").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> serviceIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        serviceId = UUID.fromString(serviceIds.getFirst());
        companyId = UUID.fromString(companyIds.getFirst());
        luciaId = jdbcTemplate.queryForObject(
                "select id from app_user where email = 'cliente1@example.com' and company_id = ?",
                UUID.class,
                companyId);
    }

    @Test
    void listShowsAllAppointmentsNewestFirstWithClientData() throws Exception {
        UUID online = saveOnline(at(2, 10), AppointmentStatus.PENDING);
        UUID manual = saveManual(at(3, 10), AppointmentStatus.CANCELLED);

        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(manual.toString()))
                .andExpect(jsonPath("$.content[0].origin").value("MANUAL"))
                .andExpect(jsonPath("$.content[0].client.id").value(nullValue()))
                .andExpect(jsonPath("$.content[0].client.name").value("Ana Gil"))
                .andExpect(jsonPath("$.content[0].client.phone").value("+5493510000123"))
                .andExpect(jsonPath("$.content[0].cancelledBy").value("COMPANY"))
                .andExpect(jsonPath("$.content[1].id").value(online.toString()))
                .andExpect(jsonPath("$.content[1].origin").value("ONLINE"))
                .andExpect(jsonPath("$.content[1].serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.content[1].client.id").value(luciaId.toString()))
                .andExpect(jsonPath("$.content[1].client.name").value("Lucía Pérez"))
                .andExpect(jsonPath("$.content[1].client.phone").value("+5493510000004"));
    }

    @Test
    void listFiltersByStatusOriginAndRescheduled() throws Exception {
        UUID pendingOnline = saveOnline(at(2, 10), AppointmentStatus.PENDING);
        UUID cancelledManual = saveManual(at(3, 10), AppointmentStatus.CANCELLED);
        UUID rescheduled = saveRescheduled(at(-2, 10));

        list("?status=CANCELLED").andExpect(ids(cancelledManual));
        list("?origin=MANUAL").andExpect(ids(cancelledManual));
        list("?rescheduled=true").andExpect(ids(rescheduled));
        list("?rescheduled=false").andExpect(ids(cancelledManual, pendingOnline));
        list("?status=PENDING&origin=ONLINE").andExpect(ids(pendingOnline));
    }

    @Test
    void listIsPaginated() throws Exception {
        saveOnline(at(2, 10), AppointmentStatus.PENDING);
        saveOnline(at(3, 10), AppointmentStatus.PENDING);
        UUID oldest = saveOnline(at(-2, 10), AppointmentStatus.COMPLETED);

        list("?page=0&size=2")
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        list("?page=1&size=2").andExpect(ids(oldest));
    }

    @Test
    void invalidPageOrFilterReturns400() throws Exception {
        list("?size=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        list("?size=101").andExpect(status().isBadRequest());
        list("?page=-1").andExpect(status().isBadRequest());
        list("?status=RESCHEDULED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void clientCannotSeeTheAgenda() throws Exception {
        String body = mockMvc.perform(post("/api/public/companies/barberia-central/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente1@example.com\",\"password\":\"%s\"}".formatted(DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String clientToken = JsonPath.read(body, "$.token");

        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void summaryShowsTodayWithoutCancelledInOrder() throws Exception {
        UUID morning = saveOnline(at(0, 8), AppointmentStatus.COMPLETED);
        saveManual(at(0, 10), AppointmentStatus.CANCELLED);
        UUID evening = saveManual(at(0, 20), AppointmentStatus.PENDING);
        saveOnline(at(1, 10), AppointmentStatus.PENDING);

        summary()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today", hasSize(2)))
                .andExpect(jsonPath("$.today[0].id").value(morning.toString()))
                .andExpect(jsonPath("$.today[1].id").value(evening.toString()));
    }

    @Test
    void summaryShowsNextFiveAfterTodayWithoutCancelled() throws Exception {
        saveOnline(at(0, 20), AppointmentStatus.PENDING);
        saveManual(at(1, 9), AppointmentStatus.CANCELLED);
        UUID first = saveOnline(at(1, 10), AppointmentStatus.PENDING);
        for (int day = 2; day <= 5; day++) {
            saveOnline(at(day, 10), AppointmentStatus.PENDING);
        }
        UUID sixth = saveOnline(at(6, 10), AppointmentStatus.PENDING);

        summary()
                .andExpect(jsonPath("$.upcoming", hasSize(5)))
                .andExpect(jsonPath("$.upcoming[0].id").value(first.toString()))
                .andExpect(
                        jsonPath("$.upcoming[4].startDateTime").value(at(5, 10).toString()))
                .andExpect(
                        jsonPath("$.upcoming[?(@.id == '%s')]".formatted(sixth)).isEmpty());
    }

    @Test
    void brandingIncludesTimezone() throws Exception {
        mockMvc.perform(get("/api/company/branding").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Argentina/Cordoba"));
    }

    private ResultActions list(String query) throws Exception {
        return mockMvc.perform(get(APPOINTMENTS + query).header("Authorization", "Bearer " + adminToken));
    }

    private ResultActions summary() throws Exception {
        return mockMvc.perform(get(APPOINTMENTS + "/summary").header("Authorization", "Bearer " + adminToken));
    }

    // Los ids de la página, en orden.
    private static ResultMatcher ids(UUID... expected) {
        return jsonPath(
                "$.content[*].id",
                contains(Stream.of(expected).map(UUID::toString).toArray(String[]::new)));
    }

    // Un día relativo a hoy, en la hora local de la empresa.
    private static Instant at(int daysFromToday, int hour) {
        return LocalDate.now(CORDOBA)
                .plusDays(daysFromToday)
                .atTime(LocalTime.of(hour, 0))
                .atZone(CORDOBA)
                .toInstant();
    }

    private UUID saveOnline(Instant start, AppointmentStatus status) {
        Appointment appointment = appointment(start, status);
        appointment.setClientId(luciaId);
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        return appointmentRepository.save(appointment).getId();
    }

    private UUID saveManual(Instant start, AppointmentStatus status) {
        Appointment appointment = appointment(start, status);
        appointment.setManualClientName("Ana Gil");
        appointment.setManualClientPhone("+5493510000123");
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        return appointmentRepository.save(appointment).getId();
    }

    private Appointment appointment(Instant start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(30);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(30)));
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.COMPANY);
            appointment.setCancelledAt(start);
        }
        return appointment;
    }

    // Turno online completado que se reprogramó una vez. El CHECK chk_appointment_reschedule exige el inicio anterior.
    private UUID saveRescheduled(Instant start) {
        Appointment appointment = appointment(start, AppointmentStatus.COMPLETED);
        appointment.setClientId(luciaId);
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        appointment.setRescheduleCount(1);
        appointment.setPreviousStartDateTime(start.minus(Duration.ofDays(1)));
        return appointmentRepository.save(appointment).getId();
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
