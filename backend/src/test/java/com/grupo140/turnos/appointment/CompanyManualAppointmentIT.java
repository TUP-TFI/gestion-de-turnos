package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.catalog.CatalogService;
import com.grupo140.turnos.catalog.ServiceRequest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga manual de turnos por el admin (US-09.5 y US-09.6) y los horarios del panel (D3), sobre Barbería Central del
 * seed. Cada test hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyManualAppointmentIT {

    private static final String APPOINTMENTS = "/api/company/appointments";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CatalogService catalogService;

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
    void adminBooksForClientWithAccountAndClientSeesIt() throws Exception {
        String start = freeStarts(serviceId).getFirst();

        String body = create("{\"serviceId\":\"%s\",\"startDateTime\":\"%s\",\"clientId\":\"%s\"}"
                        .formatted(serviceId, start, luciaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startDateTime").value(start))
                .andExpect(jsonPath("$.serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.durationMinutesSnapshot").value(30))
                .andExpect(jsonPath("$.client.id").value(luciaId.toString()))
                .andExpect(jsonPath("$.client.name").value("Lucía Pérez"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // El cliente lo ve en "Mis turnos" y lo puede gestionar como cualquier otro.
        mockMvc.perform(get("/api/client/appointments").header("Authorization", "Bearer " + clientToken()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.id == '%s')].canCancel".formatted(id)).value(true));
    }

    @Test
    void adminBooksForSomeoneWithoutAccountWithTrimmedData() throws Exception {
        String start = freeStarts(serviceId).getFirst();

        String json = "{\"serviceId\":\"%s\",\"startDateTime\":\"%s\","
                + "\"manualClientName\":\"  Ana Gil  \",\"manualClientPhone\":\" +5493510000123 \"}";
        String body = create(json.formatted(serviceId, start))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.client.id").value(nullValue()))
                .andExpect(jsonPath("$.client.name").value("Ana Gil"))
                .andExpect(jsonPath("$.client.phone").value("+5493510000123"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Appointment saved = appointmentRepository
                .findByIdAndCompanyId(UUID.fromString(JsonPath.read(body, "$.id")), companyId)
                .orElseThrow();
        assertThat(saved.getClientId()).isNull();
        assertThat(saved.getManualClientName()).isEqualTo("Ana Gil");
        assertThat(saved.getManualClientPhone()).isEqualTo("+5493510000123");
    }

    @Test
    void bookedStartLeavesPanelAvailability() throws Exception {
        String start = freeStarts(serviceId).getFirst();
        String availability = availabilityUrl(serviceId);

        mockMvc.perform(get(availability).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starts", hasItem(start)));

        create(withAccount(start)).andExpect(status().isCreated());

        mockMvc.perform(get(availability).header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.starts", not(hasItem(start))));
    }

    @Test
    void invalidIdentityReturns400() throws Exception {
        String start = freeStarts(serviceId).getFirst();
        String base = "{\"serviceId\":\"%s\",\"startDateTime\":\"%s\"".formatted(serviceId, start);

        List<String> bodies = List.of(
                // Las dos identidades a la vez.
                base + ",\"clientId\":\"%s\",\"manualClientName\":\"Ana Gil\"}".formatted(luciaId),
                // Ninguna.
                base + "}",
                // Solo el nombre.
                base + ",\"manualClientName\":\"Ana Gil\"}",
                // Nombre y teléfono en blanco.
                base + ",\"manualClientName\":\"  \",\"manualClientPhone\":\"  \"}");
        for (String body : bodies) {
            create(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CLIENT_IDENTITY"));
        }
        assertThat(appointmentRepository.findByCompanyIdAndStatus(companyId, AppointmentStatus.PENDING))
                .isEmpty();
    }

    @Test
    void clientOfAnotherCompanyOrAdminReturns404() throws Exception {
        String start = freeStarts(serviceId).getFirst();
        UUID stranger = registerClient("estetica-pendiente", "forastera@example.com");
        UUID adminId = jdbcTemplate.queryForObject(
                "select id from app_user where email = 'admin@example.com' and company_id = ?", UUID.class, companyId);

        for (UUID clientId : List.of(stranger, adminId, UUID.randomUUID())) {
            create("{\"serviceId\":\"%s\",\"startDateTime\":\"%s\",\"clientId\":\"%s\"}"
                            .formatted(serviceId, start, clientId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
        }
    }

    @Test
    void takenStartReturnsConflict() throws Exception {
        String start = freeStarts(serviceId).getFirst();
        create(withAccount(start)).andExpect(status().isCreated());

        create(withAccount(start))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void startTooSoonReturns400() throws Exception {
        Instant soon = Instant.now().plus(Duration.ofMinutes(10)).truncatedTo(ChronoUnit.MINUTES);

        create(withAccount(soon.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_SOON"));
    }

    @Test
    void inactiveServiceReturns404InBookingAndAvailability() throws Exception {
        String start = freeStarts(serviceId).getFirst();
        UUID inactive = catalogService
                .create(companyId, new ServiceRequest("Tintura", null, new BigDecimal("15000"), 30))
                .id();
        catalogService.deactivate(companyId, inactive);

        create("{\"serviceId\":\"%s\",\"startDateTime\":\"%s\",\"clientId\":\"%s\"}"
                        .formatted(inactive, start, luciaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
        mockMvc.perform(get(availabilityUrl(inactive)).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void missingServiceReturnsValidationError() throws Exception {
        create("{\"startDateTime\":\"%s\",\"clientId\":\"%s\"}"
                        .formatted(freeStarts(serviceId).getFirst(), luciaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void clientCannotUseManualBooking() throws Exception {
        String start = freeStarts(serviceId).getFirst();

        mockMvc.perform(post(APPOINTMENTS)
                        .header("Authorization", "Bearer " + clientToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withAccount(start)))
                .andExpect(status().isForbidden());
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post(APPOINTMENTS)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String withAccount(String start) {
        return "{\"serviceId\":\"%s\",\"startDateTime\":\"%s\",\"clientId\":\"%s\"}"
                .formatted(serviceId, start, luciaId);
    }

    // Próximo día hábil del seed, a más de un día: el domingo está cerrado.
    private static LocalDate nextBusinessDay() {
        LocalDate date = LocalDate.now(CORDOBA).plusDays(2);
        return date.getDayOfWeek() == DayOfWeek.SUNDAY ? date.plusDays(1) : date;
    }

    private static String availabilityUrl(UUID service) {
        return "/api/company/services/%s/availability?date=%s".formatted(service, nextBusinessDay());
    }

    // Los horarios libres del próximo día hábil, según el endpoint del panel.
    private List<String> freeStarts(UUID service) throws Exception {
        String body = mockMvc.perform(get(availabilityUrl(service)).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> starts = JsonPath.read(body, "$.starts");
        assertThat(starts).isNotEmpty();
        return starts;
    }

    // Registro de un cliente nuevo en otra empresa. Devuelve su id.
    private UUID registerClient(String slug, String email) throws Exception {
        String json = "{\"firstName\":\"Ana\",\"lastName\":\"Gil\",\"email\":\"%s\","
                + "\"phone\":\"+5493510000099\",\"password\":\"Clave.nueva.2026\"}";
        String body = mockMvc.perform(post("/api/public/companies/" + slug + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.user.id"));
    }

    private String clientToken() throws Exception {
        String body = mockMvc.perform(post("/api/public/companies/barberia-central/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente1@example.com\",\"password\":\"%s\"}".formatted(DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
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
