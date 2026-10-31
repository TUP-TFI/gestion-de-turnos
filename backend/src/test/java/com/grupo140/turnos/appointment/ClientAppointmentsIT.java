package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.ServiceRequest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * "Mis turnos" del cliente (US-07.6) de punta a punta, con los clientes del seed: {@code cliente1} y
 * {@code cliente2} de Barbería Central. Para probar el aislamiento entre empresas se registra un cliente en
 * Estética Pendiente, que no tiene ninguno.
 *
 * <p>Los turnos se guardan directo con el repositorio: hacen falta fechas y estados que la reserva no permite. Cada
 * test hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientAppointmentsIT {

    private static final String APPOINTMENTS = "/api/client/appointments";
    private static final String BARBERIA = "barberia-central";
    private static final String ESTETICA = "estetica-pendiente";
    private static final int DURATION_MINUTES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CatalogService catalogService;

    private record Client(String token, UUID userId, UUID companyId) {}

    @Test
    void clientSeesOnlyOwnAppointmentsNewestFirst() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        Client other = login(BARBERIA, "cliente2@example.com");
        UUID serviceId = seedServiceId();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID older = save(ana, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        UUID newer = save(ana, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.PENDING);
        // De otro cliente de la misma empresa: no tiene que aparecer.
        save(other, serviceId, now.plus(Duration.ofDays(4)), AppointmentStatus.PENDING);

        mockMvc.perform(listFor(ana, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.toString()))
                .andExpect(jsonPath("$[1].id").value(older.toString()));
    }

    @Test
    void clientOfAnotherCompanyDoesNotSeeTheseAppointments() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID serviceId = seedServiceId();
        Instant start = Instant.now().plus(Duration.ofDays(2));
        save(ana, serviceId, start, AppointmentStatus.PENDING);

        // Un cliente de otra empresa, con un turno propio a la misma hora.
        Client stranger = register(ESTETICA, "forastera@example.com");
        ServiceRequest request = new ServiceRequest("Limpieza facial", null, new BigDecimal("5000"), 30);
        UUID strangerService =
                catalogService.create(stranger.companyId(), request).id();
        UUID strangerAppointment = save(stranger, strangerService, start, AppointmentStatus.PENDING);

        mockMvc.perform(listFor(stranger, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(strangerAppointment.toString()))
                .andExpect(jsonPath("$[0].serviceName").value("Limpieza facial"));
    }

    @Test
    void responseCarriesServiceNameAndSnapshots() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        Instant start = Instant.now().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.SECONDS);
        save(ana, seedServiceId(), start, AppointmentStatus.PENDING);

        mockMvc.perform(listFor(ana, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].rescheduleCount").value(0))
                .andExpect(jsonPath("$[0].priceSnapshot").value(8000.0))
                .andExpect(jsonPath("$[0].durationMinutesSnapshot").value(DURATION_MINUTES))
                .andExpect(jsonPath("$[0].startDateTime").value(start.toString()));
    }

    @Test
    void statusFilterReturnsOnlyThatStatus() throws Exception {
        Client ana = login(BARBERIA, "cliente1@example.com");
        UUID serviceId = seedServiceId();
        Instant now = Instant.now();
        save(ana, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        save(ana, serviceId, now.minus(Duration.ofDays(3)), AppointmentStatus.COMPLETED);
        save(ana, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.CANCELLED);

        for (AppointmentStatus filter : AppointmentStatus.values()) {
            mockMvc.perform(listFor(ana, "?status=" + filter))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value(filter.name()));
        }
        // Sin filtro vienen los tres.
        mockMvc.perform(listFor(ana, "")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void unknownStatusReturns400() throws Exception {
        mockMvc.perform(listFor(login(BARBERIA, "cliente1@example.com"), "?status=FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void clientWithoutAppointmentsGetsEmptyList() throws Exception {
        mockMvc.perform(listFor(login(BARBERIA, "cliente2@example.com"), ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void companyAdminCannotListClientAppointments() throws Exception {
        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get(APPOINTMENTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private static MockHttpServletRequestBuilder listFor(Client client, String query) {
        return get(APPOINTMENTS + query).header("Authorization", "Bearer " + client.token());
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
        String token = adminToken();
        String services = mockMvc.perform(get("/api/company/services").header("Authorization", "Bearer " + token))
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
