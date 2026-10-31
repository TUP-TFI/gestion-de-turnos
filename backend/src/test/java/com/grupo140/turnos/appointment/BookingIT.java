package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.common.error.ApiError;
import com.grupo140.turnos.common.error.GlobalExceptionHandler;
import com.grupo140.turnos.notification.NotificationEventType;
import com.grupo140.turnos.notification.NotificationRequestedEvent;
import com.grupo140.turnos.notification.NotificationService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reserva de un turno (US-07.3, US-07.4 y US-07.5) de punta a punta, con los clientes del seed de Barbería Central.
 *
 * <p>Sin {@code @Transactional}: el lock, la constraint y el aviso después del commit solo se prueban con
 * transacciones reales. Por eso cada test limpia lo que deja en {@link #cleanUp()}.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingIT {

    private static final String APPOINTMENTS = "/api/client/appointments";
    private static final String BARBERIA = "barberia-central";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");
    private static final int DURATION_MINUTES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApplicationEventPublisher events;

    @MockitoSpyBean
    private NotificationService notificationService;

    private String clientToken;
    private UUID clientId;
    private UUID companyId;
    private UUID serviceId;

    @BeforeEach
    void setUp() throws Exception {
        String body = loginClient("cliente1@example.com");
        clientToken = JsonPath.read(body, "$.token");
        clientId = UUID.fromString(JsonPath.read(body, "$.user.id"));
        companyId = UUID.fromString(JsonPath.read(body, "$.user.companyId"));
        serviceId = seedServiceId();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "delete from notification where appointment_id in (select id from appointment where company_id = ?)",
                companyId);
        jdbcTemplate.update("delete from appointment where company_id = ?", companyId);
    }

    @Test
    void bookingCreatesPendingAppointmentWithSnapshotsAndSentNotification() throws Exception {
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));

        String body = book(clientToken, serviceId, start)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.priceSnapshot").value(8000.0))
                .andExpect(jsonPath("$.durationMinutesSnapshot").value(DURATION_MINUTES))
                .andExpect(jsonPath("$.rescheduleCount").value(0))
                .andExpect(jsonPath("$.startDateTime").value(start.toString()))
                .andExpect(jsonPath("$.endDateTime")
                        .value(start.plus(Duration.ofMinutes(DURATION_MINUTES)).toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID appointmentId = UUID.fromString(JsonPath.read(body, "$.id"));
        Map<String, Object> appointment =
                jdbcTemplate.queryForMap("select client_id, origin from appointment where id = ?", appointmentId);
        assertThat(appointment).containsEntry("client_id", clientId).containsEntry("origin", "ONLINE");

        String clientPhone =
                jdbcTemplate.queryForObject("select phone from app_user where id = ?", String.class, clientId);
        assertThat(notificationOf(appointmentId))
                .containsEntry("event_type", "BOOKING_CONFIRMED")
                .containsEntry("channel", "WHATSAPP")
                .containsEntry("status", "SENT")
                .containsEntry("attempt_count", 1)
                .containsEntry("recipient_phone", clientPhone)
                .containsEntry("user_id", clientId);
        assertThat(notificationOf(appointmentId).get("sent_at")).isNotNull();
    }

    @Test
    void failedDispatchKeepsAppointmentAndLeavesNotificationFailed() throws Exception {
        doThrow(new IllegalStateException("El proveedor no responde"))
                .when(notificationService)
                .dispatch(any(), any());

        String body = book(clientToken, serviceId, nextBusinessDayAt(LocalTime.of(10, 0)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(notificationOf(UUID.fromString(JsonPath.read(body, "$.id"))))
                .containsEntry("status", "FAILED")
                .containsEntry("attempt_count", 1)
                .containsEntry("error_message", "El proveedor no responde")
                .containsEntry("sent_at", null);
    }

    // Un Error no lo atrapa el listener y revierte solo la transacción del envío, igual que si el proceso se cortara
    // antes de despachar: la fila confirmada con la reserva sigue en PENDING para el reintento de US-10.4.
    @Test
    void pendingNotificationSurvivesWhenDispatchDoesNotFinish() throws Exception {
        doThrow(new Error("El proceso se cortó")).when(notificationService).dispatch(any(), any());

        String body = book(clientToken, serviceId, nextBusinessDayAt(LocalTime.of(10, 0)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(notificationOf(UUID.fromString(JsonPath.read(body, "$.id"))))
                .containsEntry("status", "PENDING")
                .containsEntry("attempt_count", 0)
                .containsEntry("sent_at", null);
    }

    @Test
    void rolledBackTransactionLeavesNoNotificationAndDispatchesNothing() {
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID appointmentId = transactionTemplate.execute(status -> {
            UUID id = appointmentRepository.saveAndFlush(appointmentAt(start)).getId();
            events.publishEvent(new NotificationRequestedEvent(
                    id, clientId, "+5493510000001", NotificationEventType.BOOKING_CONFIRMED, "Aviso de prueba"));
            // Dentro de la transacción, la fila ya existe en PENDING.
            assertThat(notificationOf(id)).containsEntry("status", "PENDING").containsEntry("attempt_count", 0);
            status.setRollbackOnly();
            return id;
        });

        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from notification where appointment_id = ?", Long.class, appointmentId))
                .isZero();
        verify(notificationService, never()).dispatch(any(), any());
    }

    @Test
    void startInsideMinimumNoticeReturnsTooSoon() throws Exception {
        book(clientToken, serviceId, Instant.now().plus(Duration.ofMinutes(5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_SOON"));
    }

    @Test
    void pastStartReturnsTooSoon() throws Exception {
        book(clientToken, serviceId, Instant.now().minus(Duration.ofDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_SOON"));
    }

    @Test
    void startBeyondMaximumAdvanceReturnsTooFar() throws Exception {
        book(clientToken, serviceId, Instant.now().plus(Duration.ofDays(400)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_FAR"));
    }

    @Test
    void occupiedStartReturnsConflict() throws Exception {
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        book(clientToken, serviceId, start).andExpect(status().isCreated());

        book(clientToken, serviceId, start)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void startOutsideBusinessHoursReturnsConflict() throws Exception {
        book(clientToken, serviceId, nextBusinessDayAt(LocalTime.of(23, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void startOutsideIntervalGridReturnsConflict() throws Exception {
        book(clientToken, serviceId, nextBusinessDayAt(LocalTime.of(10, 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void unknownServiceReturns404() throws Exception {
        book(clientToken, UUID.randomUUID(), nextBusinessDayAt(LocalTime.of(10, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mockMvc.perform(post(APPOINTMENTS)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void companyAdminCannotBook() throws Exception {
        book(adminToken(), serviceId, nextBusinessDayAt(LocalTime.of(10, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void concurrentBookingsOfSameStartLetOnlyOneThrough() throws Exception {
        String otherToken = JsonPath.read(loginClient("cliente2@example.com"), "$.token");
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (String token : List.of(clientToken, otherToken)) {
            attempts.add(() -> {
                ready.countDown();
                go.await();
                return book(token, serviceId, start).andReturn().getResponse().getStatus();
            });
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> attempt : attempts) {
                futures.add(executor.submit(attempt));
            }
            ready.await();
            go.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(appointmentRepository.findByCompanyIdAndStatus(companyId, AppointmentStatus.PENDING))
                .hasSize(1);
    }

    @Test
    void overlappingInsertTriggersConstraintAndHandlerMapsItToConflict() {
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        appointmentRepository.saveAndFlush(appointmentAt(start));

        // Entra directo al repositorio, sin el lock ni la validación del service: solo la frena la constraint.
        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(appointmentAt(start.plus(Duration.ofMinutes(15)))))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, ex -> {
                    ResponseEntity<ApiError> response = new GlobalExceptionHandler().handleDataIntegrity(ex);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody().code()).isEqualTo("SLOT_UNAVAILABLE");
                });
    }

    private Appointment appointmentAt(Instant start) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
        appointment.setClientId(clientId);
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(DURATION_MINUTES);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(DURATION_MINUTES)));
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointment;
    }

    private Map<String, Object> notificationOf(UUID appointmentId) {
        return jdbcTemplate.queryForMap(
                "select event_type, channel, status, attempt_count, recipient_phone, user_id, sent_at, error_message"
                        + " from notification where appointment_id = ?",
                appointmentId);
    }

    // Próximo día hábil del seed, a más de un día: el domingo está cerrado y los demás días abren a la mañana.
    private static Instant nextBusinessDayAt(LocalTime time) {
        LocalDate date = LocalDate.now(CORDOBA).plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(time).atZone(CORDOBA).toInstant();
    }

    private ResultActions book(String token, UUID service, Instant start) throws Exception {
        return mockMvc.perform(post(APPOINTMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceId\":\"%s\",\"startDateTime\":\"%s\"}".formatted(service, start)));
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

    private String loginClient(String email) throws Exception {
        return mockMvc.perform(post("/api/public/companies/" + BARBERIA + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
