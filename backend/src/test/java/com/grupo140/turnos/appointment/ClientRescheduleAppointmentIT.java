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
 * Reprogramación de un turno por su cliente (US-08.2) de punta a punta, con la configuración del seed: plazo de 3
 * horas, tope de 2 reprogramaciones, 30 minutos de anticipación mínima y 90 días de máxima. Los turnos se guardan
 * directo con el repositorio, porque hacen falta fechas, estados y contadores que la reserva no permite. Cada test
 * hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientRescheduleAppointmentIT {

    private static final String APPOINTMENTS = "/api/client/appointments";
    private static final String BARBERIA = "barberia-central";
    private static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record Client(String token, UUID userId, UUID companyId) {}

    @Test
    void clientReschedulesSameAppointmentWithoutNotification() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant oldStart = nextBusinessDayAt(LocalTime.of(10, 0));
        Instant newStart = nextBusinessDayAt(LocalTime.of(11, 0));
        UUID id = save(ana, seedServiceId(), oldStart, 30, AppointmentStatus.PENDING);

        reschedule(ana, id, newStart)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startDateTime").value(newStart.toString()))
                .andExpect(jsonPath("$.endDateTime")
                        .value(newStart.plus(Duration.ofMinutes(30)).toString()))
                .andExpect(jsonPath("$.rescheduleCount").value(1))
                .andExpect(jsonPath("$.canReschedule").value(true))
                .andExpect(jsonPath("$.rescheduleBlockedReason").value(nullValue()));

        Appointment rescheduled = find(ana, id);
        assertThat(rescheduled.getPreviousStartDateTime()).isEqualTo(oldStart);
        assertThat(notificationCount(id)).isZero();
    }

    // El turno de 60 minutos se corre 30 dentro de su propio horario, y el fin sale de la duración copiada, no de los
    // 30 minutos actuales del servicio.
    @Test
    void appointmentMovesInsideItsOwnSlotWithItsCopiedDuration() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant oldStart = nextBusinessDayAt(LocalTime.of(10, 0));
        Instant newStart = nextBusinessDayAt(LocalTime.of(10, 30));
        UUID id = save(ana, seedServiceId(), oldStart, 60, AppointmentStatus.PENDING);

        reschedule(ana, id, newStart)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDateTime")
                        .value(newStart.plus(Duration.ofMinutes(60)).toString()));
    }

    @Test
    void availabilityIgnoresTheAppointmentItselfAndHidesItsCurrentStart() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = save(ana, seedServiceId(), start, 60, AppointmentStatus.PENDING);

        availability(ana, id, start)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(localDate(start).toString()))
                .andExpect(jsonPath(
                        "$.starts",
                        hasItem(nextBusinessDayAt(LocalTime.of(10, 30)).toString())))
                .andExpect(jsonPath("$.starts", not(hasItem(start.toString()))));
    }

    // Regla 23: el servicio desactivado ya no ofrece horarios a una reserva nueva, pero el turno se puede reprogramar.
    @Test
    void appointmentOfInactiveServiceCanBeRescheduled() throws Exception {
        Client ana = login("cliente1@example.com");
        UUID serviceId = seedServiceId();
        Instant newStart = nextBusinessDayAt(LocalTime.of(11, 0));
        UUID id = save(ana, serviceId, nextBusinessDayAt(LocalTime.of(10, 0)), 30, AppointmentStatus.PENDING);
        mockMvc.perform(post("/api/company/services/" + serviceId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/companies/%s/services/%s/availability?date=%s"
                        .formatted(BARBERIA, serviceId, localDate(newStart))))
                .andExpect(status().isNotFound());
        availability(ana, id, newStart).andExpect(jsonPath("$.starts", hasItem(newStart.toString())));
        reschedule(ana, id, newStart).andExpect(status().isOk());
    }

    @Test
    void appointmentOfAnotherClientReturns404() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = save(ana, seedServiceId(), start, 30, AppointmentStatus.PENDING);
        Client martin = login("cliente2@example.com");

        reschedule(martin, id, nextBusinessDayAt(LocalTime.of(11, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
        availability(martin, id, start).andExpect(status().isNotFound());

        assertThat(find(ana, id).getStartDateTime()).isEqualTo(start);
    }

    @Test
    void cancelledAppointmentReturnsConflict() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = save(ana, seedServiceId(), start, 30, AppointmentStatus.CANCELLED);

        reschedule(ana, id, nextBusinessDayAt(LocalTime.of(11, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
        availability(ana, id, start)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_PENDING"));
    }

    @Test
    void appointmentPastDeadlineReturns400() throws Exception {
        Client ana = login("cliente1@example.com");
        UUID id = save(ana, seedServiceId(), Instant.now().plus(Duration.ofHours(2)), 30, AppointmentStatus.PENDING);

        reschedule(ana, id, nextBusinessDayAt(LocalTime.of(11, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_DEADLINE_PASSED"));
    }

    @Test
    void appointmentAtLimitReturnsConflict() throws Exception {
        Client ana = login("cliente1@example.com");
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        UUID id = save(ana, seedServiceId(), start, 30, AppointmentStatus.PENDING);
        markRescheduled(ana, id, 2);

        reschedule(ana, id, nextBusinessDayAt(LocalTime.of(11, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESCHEDULE_LIMIT_REACHED"));

        assertThat(find(ana, id).getStartDateTime()).isEqualTo(start);
    }

    @Test
    void newStartOutsideBookingWindowReturns400() throws Exception {
        Client ana = login("cliente1@example.com");
        UUID id = save(ana, seedServiceId(), nextBusinessDayAt(LocalTime.of(10, 0)), 30, AppointmentStatus.PENDING);

        reschedule(ana, id, Instant.now().plus(Duration.ofMinutes(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_SOON"));
        reschedule(ana, id, Instant.now().plus(Duration.ofDays(100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOKING_TOO_FAR"));
    }

    @Test
    void occupiedOrSameStartReturnsConflict() throws Exception {
        Client ana = login("cliente1@example.com");
        UUID serviceId = seedServiceId();
        Instant start = nextBusinessDayAt(LocalTime.of(10, 0));
        Instant taken = nextBusinessDayAt(LocalTime.of(11, 0));
        UUID id = save(ana, serviceId, start, 30, AppointmentStatus.PENDING);
        save(login("cliente2@example.com"), serviceId, taken, 30, AppointmentStatus.PENDING);

        reschedule(ana, id, taken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        reschedule(ana, id, start)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));

        assertThat(find(ana, id).getRescheduleCount()).isZero();
    }

    @Test
    void listTellsWhichAppointmentsCanBeRescheduledAndWhy() throws Exception {
        Client ana = login("cliente1@example.com");
        UUID serviceId = seedServiceId();
        UUID atLimit = save(ana, serviceId, inDays(3), 30, AppointmentStatus.PENDING);
        markRescheduled(ana, atLimit, 2);
        UUID allowed = save(ana, serviceId, inDays(2), 30, AppointmentStatus.PENDING);
        UUID pastDeadline =
                save(ana, serviceId, Instant.now().plus(Duration.ofHours(2)), 30, AppointmentStatus.PENDING);
        UUID cancelled = save(ana, serviceId, inDays(-1), 30, AppointmentStatus.CANCELLED);

        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + ana.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(atLimit.toString()))
                .andExpect(jsonPath("$[0].canReschedule").value(false))
                .andExpect(jsonPath("$[0].rescheduleBlockedReason").value("LIMIT_REACHED"))
                .andExpect(jsonPath("$[1].id").value(allowed.toString()))
                .andExpect(jsonPath("$[1].canReschedule").value(true))
                .andExpect(jsonPath("$[1].rescheduleBlockedReason").value(nullValue()))
                .andExpect(jsonPath("$[2].id").value(pastDeadline.toString()))
                .andExpect(jsonPath("$[2].rescheduleBlockedReason").value("DEADLINE_PASSED"))
                .andExpect(jsonPath("$[3].id").value(cancelled.toString()))
                .andExpect(jsonPath("$[3].rescheduleBlockedReason").value("NOT_PENDING"));
    }

    private ResultActions reschedule(Client client, UUID id, Instant start) throws Exception {
        return mockMvc.perform(post(APPOINTMENTS + "/" + id + "/reschedule")
                .header("Authorization", "Bearer " + client.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDateTime\":\"%s\"}".formatted(start)));
    }

    private ResultActions availability(Client client, UUID id, Instant onDateOf) throws Exception {
        return mockMvc.perform(get(APPOINTMENTS + "/" + id + "/availability?date=" + localDate(onDateOf))
                .header("Authorization", "Bearer " + client.token()));
    }

    private Appointment find(Client client, UUID id) {
        return appointmentRepository
                .findByIdAndCompanyIdAndClientId(id, client.companyId(), client.userId())
                .orElseThrow();
    }

    // Lo exige el CHECK chk_appointment_reschedule: con reprogramaciones, tiene que haber un inicio anterior.
    private void markRescheduled(Client client, UUID id, int count) {
        Appointment appointment = find(client, id);
        appointment.setRescheduleCount(count);
        appointment.setPreviousStartDateTime(appointment.getStartDateTime().minus(Duration.ofDays(1)));
    }

    private int notificationCount(UUID appointmentId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notification where appointment_id = ?", Integer.class, appointmentId);
    }

    private static LocalDate localDate(Instant instant) {
        return instant.atZone(CORDOBA).toLocalDate();
    }

    private static Instant inDays(int days) {
        return Instant.now().plus(Duration.ofDays(days)).truncatedTo(ChronoUnit.SECONDS);
    }

    // Próximo día hábil del seed, a más de un día: el domingo está cerrado y los demás días abren de 9 a 13.
    private static Instant nextBusinessDayAt(LocalTime time) {
        LocalDate date = LocalDate.now(CORDOBA).plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(time).atZone(CORDOBA).toInstant();
    }

    // Turno online del cliente, guardado directo con el repositorio. Devuelve su id.
    private UUID save(Client client, UUID serviceId, Instant start, int durationMinutes, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(client.companyId());
        appointment.setServiceId(serviceId);
        appointment.setClientId(client.userId());
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(durationMinutes);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(durationMinutes)));
        appointment.setOrigin(AppointmentOrigin.ONLINE);
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.CLIENT);
            appointment.setCancelledAt(Instant.now());
        }
        return appointmentRepository.save(appointment).getId();
    }

    // El servicio "Corte de pelo" de Barbería Central, que dura 30 minutos en el seed.
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

    private Client login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/public/companies/" + BARBERIA + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new Client(
                JsonPath.read(body, "$.token"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                UUID.fromString(JsonPath.read(body, "$.user.companyId")));
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
