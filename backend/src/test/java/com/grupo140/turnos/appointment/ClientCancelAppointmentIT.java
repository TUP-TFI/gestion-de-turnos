package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelación de un turno por su cliente (US-08.1) de punta a punta, con el plazo de 3 horas del seed. Los turnos se
 * guardan directo con el repositorio, porque hacen falta fechas y estados que la reserva no permite. Cada test hace
 * rollback ({@code @Transactional}). El borde exacto del plazo se prueba en {@code AppointmentServiceTest}.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientCancelAppointmentIT {

    private static final String APPOINTMENTS = "/api/client/appointments";
    private static final String BARBERIA = "barberia-central";
    private static final String ESTETICA = "estetica-pendiente";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");
    private static final int DURATION_MINUTES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record Client(String token, UUID userId, UUID companyId) {}

    @Test
    void clientCancelsOwnAppointmentWithoutNotification() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(2), AppointmentStatus.PENDING);

        cancel(ana, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.canCancel").value(false));

        Appointment cancelled = find(ana, id);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy()).isEqualTo(CancelledBy.CLIENT);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancellationReason()).isNull();
        assertThat(notificationCount(id)).isZero();
    }

    @Test
    void cancelledStartIsAvailableAgain() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID serviceId = seedServiceId();
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = save(ana, serviceId, start, AppointmentStatus.PENDING);
        String availability = "/api/public/companies/%s/services/%s/availability?date=%s"
                .formatted(BARBERIA, serviceId, start.atZone(CORDOBA).toLocalDate());

        mockMvc.perform(get(availability)).andExpect(jsonPath("$.starts", not(hasItem(start.toString()))));

        cancel(ana, id).andExpect(status().isOk());

        mockMvc.perform(get(availability)).andExpect(jsonPath("$.starts", hasItem(start.toString())));
    }

    @Test
    void appointmentOfAnotherClientReturns404() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(2), AppointmentStatus.PENDING);

        cancel(login(BARBERIA, "cliente2@example.com"), id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));

        assertThat(find(ana, id).getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void appointmentOfAnotherCompanyReturns404() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(2), AppointmentStatus.PENDING);

        cancel(register(ESTETICA, "forastera@example.com"), id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));

        assertThat(find(ana, id).getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void cancelledAppointmentReturnsConflict() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(2), AppointmentStatus.CANCELLED);

        cancel(ana, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
    }

    @Test
    void completedAppointmentReturnsConflict() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(-2), AppointmentStatus.COMPLETED);

        cancel(ana, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
    }

    @Test
    void appointmentPastDeadlineReturns400AndStaysPending() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), Instant.now().plus(Duration.ofHours(2)), AppointmentStatus.PENDING);

        cancel(ana, id)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANCELLATION_DEADLINE_PASSED"));

        assertThat(find(ana, id).getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void listTellsWhichAppointmentsCanBeCancelled() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID serviceId = seedServiceId();
        UUID insideDeadline = save(ana, serviceId, inDays(2), AppointmentStatus.PENDING);
        UUID pastDeadline = save(ana, serviceId, Instant.now().plus(Duration.ofHours(2)), AppointmentStatus.PENDING);

        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(insideDeadline.toString()))
                .andExpect(jsonPath("$[0].canCancel").value(true))
                .andExpect(jsonPath("$[1].id").value(pastDeadline.toString()))
                .andExpect(jsonPath("$[1].canCancel").value(false));
    }

    @Test
    void companyAdminCannotCancelThroughClientEndpoint() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID id = save(ana, seedServiceId(), inDays(2), AppointmentStatus.PENDING);

        mockMvc.perform(post(APPOINTMENTS + "/" + id + "/cancel").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions cancel(Client client, UUID id) throws Exception {
        return mockMvc.perform(
                post(APPOINTMENTS + "/" + id + "/cancel").header("Authorization", "Bearer " + client.token()));
    }

    private Appointment find(Client client, UUID id) {
        return appointmentRepository
                .findByIdAndCompanyIdAndClientId(id, client.companyId(), client.userId())
                .orElseThrow();
    }

    private int notificationCount(UUID appointmentId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notification where appointment_id = ?", Integer.class, appointmentId);
    }

    private static Instant inDays(int days) {
        return Instant.now().plus(Duration.ofDays(days)).truncatedTo(ChronoUnit.SECONDS);
    }

    // Próximo día hábil del seed, a más de un día: el domingo está cerrado y los demás días abren a la mañana.
    private static Instant nextBusinessDayAt(LocalTime time) {
        LocalDate date = LocalDate.now(CORDOBA).plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(time).atZone(CORDOBA).toInstant();
    }

    // Turno online del cliente, guardado directo con el repositorio. Devuelve su id.
    private UUID save(Client client, UUID serviceId, Instant start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(client.companyId());
        appointment.setServiceId(serviceId);
        appointment.setClientId(client.userId());
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(DURATION_MINUTES);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(DURATION_MINUTES)));
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.CLIENT);
            appointment.setCancelledAt(Instant.now());
        }
        return appointmentRepository.save(appointment).getId();
    }

    // El servicio "Corte de pelo" de Barbería Central, que es de la empresa del admin del seed.
    private UUID seedServiceId() throws Exception {
        String services = mockMvc.perform(
                        get("/api/company/services").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        return UUID.fromString(ids.getFirst());
    }

    private Client login(String slug, String email) throws Exception {
        String body = mockMvc.perform(post("/api/public/companies/" + slug + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return toClient(body);
    }

    // Registro de un cliente nuevo: queda con la sesión iniciada.
    private Client register(String slug, String email) throws Exception {
        String json = "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"%s\","
                + "\"phone\":\"+5493510000099\",\"password\":\"Clave.nueva.2026\"}";
        String body = mockMvc.perform(post("/api/public/companies/" + slug + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return toClient(body);
    }

    private static Client toClient(String loginBody) {
        return new Client(
                JsonPath.read(loginBody, "$.token"),
                UUID.fromString(JsonPath.read(loginBody, "$.user.id")),
                UUID.fromString(JsonPath.read(loginBody, "$.user.companyId")));
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
