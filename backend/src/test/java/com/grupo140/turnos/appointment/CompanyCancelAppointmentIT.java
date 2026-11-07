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
import java.time.LocalTime;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelación de un turno por el admin (US-09.3) sobre Barbería Central del seed. Los turnos se guardan directo con el
 * repositorio, porque hacen falta fechas y estados que la reserva no permite. Cada test hace rollback
 * ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyCancelAppointmentIT {

    private static final String APPOINTMENTS = "/api/company/appointments";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");
    private static final int DURATION_MINUTES = 30;

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
    void adminCancelsWithTrimmedReason() throws Exception {
        UUID id = saveOnline(inDays(2), AppointmentStatus.PENDING);

        cancel(id, "{\"reason\":\"  Corte de luz en el local  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("COMPANY"))
                .andExpect(jsonPath("$.cancellationReason").value("Corte de luz en el local"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.client.name").value("Lucía Pérez"));

        Appointment cancelled = find(id);
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy()).isEqualTo(CancelledBy.COMPANY);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancellationReason()).isEqualTo("Corte de luz en el local");
    }

    @Test
    void withoutBodyOrWithBlankReasonSavesNoReason() throws Exception {
        UUID withoutBody = saveOnline(inDays(2), AppointmentStatus.PENDING);
        UUID blankReason = saveOnline(inDays(3), AppointmentStatus.PENDING);

        cancel(withoutBody, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").value(nullValue()));
        cancel(blankReason, "{\"reason\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").value(nullValue()));

        assertThat(find(withoutBody).getCancellationReason()).isNull();
        assertThat(find(blankReason).getCancellationReason()).isNull();
    }

    @Test
    void ignoresClientDeadlineEvenIfAlreadyStarted() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID inOneHour = saveOnline(now.plus(Duration.ofHours(1)), AppointmentStatus.PENDING);
        UUID started = saveOnline(now.minus(Duration.ofMinutes(10)), AppointmentStatus.PENDING);

        cancel(inOneHour, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel(started, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminCancelsManualAppointment() throws Exception {
        UUID id = saveManual(companyId, serviceId, inDays(2));

        cancel(id, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.client.id").value(nullValue()))
                .andExpect(jsonPath("$.client.name").value("Ana Gil"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelledStartIsAvailableAgain() throws Exception {
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = saveOnline(start, AppointmentStatus.PENDING);
        String availability = "/api/public/companies/barberia-central/services/%s/availability?date=%s"
                .formatted(serviceId, start.atZone(CORDOBA).toLocalDate());

        mockMvc.perform(get(availability)).andExpect(jsonPath("$.starts", not(hasItem(start.toString()))));

        cancel(id, null).andExpect(status().isOk());

        mockMvc.perform(get(availability)).andExpect(jsonPath("$.starts", hasItem(start.toString())));
    }

    @Test
    void unknownAppointmentReturns404() throws Exception {
        cancel(UUID.randomUUID(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
    }

    @Test
    void appointmentOfAnotherCompanyReturns404() throws Exception {
        UUID esteticaId =
                jdbcTemplate.queryForObject("select id from company where slug = 'estetica-pendiente'", UUID.class);
        ServiceRequest request = new ServiceRequest("Limpieza facial", null, new BigDecimal("5000"), DURATION_MINUTES);
        UUID esteticaService = catalogService.create(esteticaId, request).id();
        UUID id = saveManual(esteticaId, esteticaService, inDays(2));

        cancel(id, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));

        assertThat(appointmentRepository
                        .findByIdAndCompanyId(id, esteticaId)
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void cancelledOrCompletedAppointmentReturnsConflict() throws Exception {
        UUID cancelled = saveOnline(inDays(2), AppointmentStatus.CANCELLED);
        UUID completed = saveOnline(inDays(-2), AppointmentStatus.COMPLETED);

        cancel(cancelled, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
        cancel(completed, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
    }

    @Test
    void reasonLongerThan200Returns400AndStaysPending() throws Exception {
        UUID id = saveOnline(inDays(2), AppointmentStatus.PENDING);

        cancel(id, "{\"reason\":\"%s\"}".formatted("x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(find(id).getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void clientCannotCancelThroughAdminEndpoint() throws Exception {
        UUID id = saveOnline(inDays(2), AppointmentStatus.PENDING);
        String body = mockMvc.perform(post("/api/public/companies/barberia-central/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente1@example.com\",\"password\":\"%s\"}".formatted(DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String clientToken = JsonPath.read(body, "$.token");

        mockMvc.perform(post(APPOINTMENTS + "/" + id + "/cancel").header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        assertThat(find(id).getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    // Sin json, la request va sin body.
    private ResultActions cancel(UUID id, String json) throws Exception {
        MockHttpServletRequestBuilder request =
                post(APPOINTMENTS + "/" + id + "/cancel").header("Authorization", "Bearer " + adminToken);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    private Appointment find(UUID id) {
        return appointmentRepository.findByIdAndCompanyId(id, companyId).orElseThrow();
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

    private UUID saveOnline(Instant start, AppointmentStatus status) {
        Appointment appointment = appointment(companyId, serviceId, start, status);
        appointment.setClientId(luciaId);
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        return appointmentRepository.save(appointment).getId();
    }

    // Turno manual pendiente, sin cuenta, en la empresa y el servicio que se pasan.
    private UUID saveManual(UUID company, UUID service, Instant start) {
        Appointment appointment = appointment(company, service, start, AppointmentStatus.PENDING);
        appointment.setManualClientName("Ana Gil");
        appointment.setManualClientPhone("+5493510000123");
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        return appointmentRepository.save(appointment).getId();
    }

    private static Appointment appointment(UUID company, UUID service, Instant start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(company);
        appointment.setServiceId(service);
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(DURATION_MINUTES);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(DURATION_MINUTES)));
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.CLIENT);
            appointment.setCancelledAt(Instant.now());
        }
        return appointment;
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
