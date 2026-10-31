package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cierre de turnos (US-11.1) sobre Barbería Central del seed, llamando al método directo, sin esperar al scheduler.
 *
 * <p>Sin {@code @Transactional}: el update masivo no pasa por las entidades cargadas, así que el resultado se lee de
 * la base con {@link JdbcTemplate}. Por eso cada test limpia lo que deja en {@link #cleanUp()}.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompleteAppointmentsIT {

    private static final Instant OLD_UPDATE = Instant.parse("2020-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID companyId;
    private UUID serviceId;

    @BeforeEach
    void setUp() throws Exception {
        String token = adminToken();
        String services = mockMvc.perform(get("/api/company/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> serviceIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        serviceId = UUID.fromString(serviceIds.getFirst());
        companyId = UUID.fromString(companyIds.getFirst());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from appointment where company_id = ?", companyId);
    }

    @Test
    void finishedPendingBecomesCompletedAndUpdatesTimestamp() {
        Instant now = Instant.now();
        UUID finished = save(now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), AppointmentStatus.PENDING);

        assertThat(appointmentService.completeFinished()).isEqualTo(1);

        assertThat(statusOf(finished)).isEqualTo("COMPLETED");
        assertThat(updatedAtOf(finished)).isAfter(OLD_UPDATE);
    }

    @Test
    void rescheduledFinishedAppointmentIsCompletedAndKeepsItsCount() {
        Instant now = Instant.now();
        UUID finished = save(now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), AppointmentStatus.PENDING);
        jdbcTemplate.update(
                "update appointment set reschedule_count = 1, previous_start_date_time = ? where id = ?",
                Timestamp.from(now.minus(Duration.ofDays(1))),
                finished);

        appointmentService.completeFinished();

        assertThat(statusOf(finished)).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                        "select reschedule_count from appointment where id = ?", Integer.class, finished))
                .isEqualTo(1);
    }

    @Test
    void appointmentInProgressStaysPending() {
        Instant now = Instant.now();
        UUID inProgress =
                save(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofMinutes(20)), AppointmentStatus.PENDING);

        assertThat(appointmentService.completeFinished()).isZero();

        assertThat(statusOf(inProgress)).isEqualTo("PENDING");
        assertThat(updatedAtOf(inProgress)).isEqualTo(OLD_UPDATE);
    }

    @Test
    void futureAppointmentStaysPending() {
        Instant now = Instant.now();
        Instant start = now.plus(Duration.ofDays(1));
        UUID future = save(start, start.plus(Duration.ofMinutes(30)), AppointmentStatus.PENDING);

        assertThat(appointmentService.completeFinished()).isZero();

        assertThat(statusOf(future)).isEqualTo("PENDING");
    }

    @Test
    void finishedCancelledAppointmentStaysCancelled() {
        Instant now = Instant.now();
        UUID cancelled =
                save(now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), AppointmentStatus.CANCELLED);

        assertThat(appointmentService.completeFinished()).isZero();

        assertThat(statusOf(cancelled)).isEqualTo("CANCELLED");
        assertThat(updatedAtOf(cancelled)).isEqualTo(OLD_UPDATE);
    }

    // Con un instante fijo se prueba el corte exacto: el turno que termina justo ahora ya está completado, y
    // updatedAt queda con ese mismo instante.
    @Test
    void cutoffIsInclusiveOnEndDateTimeAndWritesUpdatedAt() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID endsNow = save(now.minus(Duration.ofMinutes(30)), now, AppointmentStatus.PENDING);
        UUID endsLater = save(now, now.plus(Duration.ofSeconds(1)), AppointmentStatus.PENDING);

        Integer completed = transactionTemplate.execute(status -> appointmentRepository.completeFinished(now));

        assertThat(completed).isEqualTo(1);
        assertThat(statusOf(endsNow)).isEqualTo("COMPLETED");
        assertThat(updatedAtOf(endsNow)).isEqualTo(now);
        assertThat(statusOf(endsLater)).isEqualTo("PENDING");
    }

    // Turno cargado a mano, guardado directo con el repositorio, con un updated_at viejo para ver si el job lo toca.
    private UUID save(Instant start, Instant end, AppointmentStatus appointmentStatus) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
        appointment.setManualClientName("Cliente de prueba");
        appointment.setManualClientPhone("+5493510000123");
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(30);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setStatus(appointmentStatus);
        if (appointmentStatus == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.COMPANY);
            appointment.setCancelledAt(start);
        }
        UUID id = appointmentRepository.save(appointment).getId();
        jdbcTemplate.update("update appointment set updated_at = ? where id = ?", Timestamp.from(OLD_UPDATE), id);
        return id;
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject("select status from appointment where id = ?", String.class, id);
    }

    private Instant updatedAtOf(UUID id) {
        return jdbcTemplate
                .queryForObject("select updated_at from appointment where id = ?", OffsetDateTime.class, id)
                .toInstant();
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
