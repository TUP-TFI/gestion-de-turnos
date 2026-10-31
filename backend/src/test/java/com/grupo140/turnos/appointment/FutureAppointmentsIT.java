package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conteos de turnos con turnos reales en la base, sobre la empresa del seed: futuros de un servicio (US-05.2),
 * fuera de un horario semanal (US-05.4) y afectados por una excepción de calendario (US-05.6).
 *
 * <p>Los turnos se guardan directo con el repositorio: hacen falta fechas y estados que la reserva no permite. Cada
 * test hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FutureAppointmentsIT {

    private static final String SERVICES = "/api/company/services";
    private static final String EXCEPTIONS = "/api/company/schedule-exceptions";
    private static final int DURATION_MINUTES = 30;
    // Zona por defecto de la empresa del seed.
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Cordoba");
    private static final LocalDate DAY = LocalDate.now(ZONE).plusDays(5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void countsOnlyFuturePendingAppointments() throws Exception {
        String token = adminToken();
        SeedService service = seedService(token);

        Instant now = Instant.now();
        // Cuenta: pendiente y futuro.
        save(service, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        // No cuentan: uno ya pasado y uno futuro pero cancelado.
        save(service, now.minus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        save(service, now.plus(Duration.ofDays(3)), AppointmentStatus.CANCELLED);

        mockMvc.perform(get(SERVICES + "/" + service.serviceId() + "/future-appointments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void affectedByExceptionCountsOnlyAppointmentsOfThatDateOutsideIt() throws Exception {
        String token = adminToken();
        SeedService service = seedService(token);
        save(service, at(DAY, 10), AppointmentStatus.PENDING);
        save(service, at(DAY, 17), AppointmentStatus.PENDING);
        // Otro día: no lo toca la excepción.
        save(service, at(DAY.plusDays(1), 10), AppointmentStatus.PENDING);

        String closedDay = "{\"exceptionDate\": \"%s\", \"isClosed\": true}".formatted(DAY);
        countAffected(token, closedDay).andExpect(jsonPath("$.count").value(2));

        // Abre solo de 09:00 a 12:00: queda afuera el de las 17:00.
        String morningOnly = ("{\"exceptionDate\": \"%s\", \"isClosed\": false, \"startTime\": \"09:00\","
                        + " \"endTime\": \"12:00\", \"intervalMinutes\": 30}")
                .formatted(DAY);
        countAffected(token, morningOnly).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void outOfRangeSkipsDatesWithAnException() throws Exception {
        String token = adminToken();
        SeedService service = seedService(token);
        save(service, at(DAY, 10), AppointmentStatus.PENDING);
        save(service, at(DAY.plusDays(1), 10), AppointmentStatus.PENDING);
        mockMvc.perform(post(EXCEPTIONS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exceptionDate\": \"%s\", \"isClosed\": true}".formatted(DAY)))
                .andExpect(status().isCreated());

        // Con la semana entera cerrada, solo cuenta el turno del día sin excepción.
        mockMvc.perform(post("/api/company/business-hours/out-of-range-appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hours\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    private ResultActions countAffected(String token, String body) throws Exception {
        return mockMvc.perform(post(EXCEPTIONS + "/affected-appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // Hora local de la empresa en una fecha, pasada a instante.
    private static Instant at(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(ZONE).toInstant();
    }

    private record SeedService(UUID companyId, UUID serviceId) {}

    // El servicio "Corte de pelo" del seed, con su empresa.
    private SeedService seedService(String token) throws Exception {
        String services = mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        return new SeedService(UUID.fromString(companyIds.getFirst()), UUID.fromString(ids.getFirst()));
    }

    // Turno manual (sin cuenta), para no depender de los clientes del seed.
    private void save(SeedService service, Instant start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(service.companyId());
        appointment.setServiceId(service.serviceId());
        appointment.setManualClientName("Cliente de prueba");
        appointment.setManualClientPhone("+5493510000123");
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(DURATION_MINUTES);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(DURATION_MINUTES)));
        appointment.setOrigin(AppointmentOrigin.MANUAL);
        appointment.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) {
            // Lo exige el CHECK chk_appointment_cancellation.
            appointment.setCancelledBy(CancelledBy.COMPANY);
            appointment.setCancelledAt(Instant.now());
        }
        appointmentRepository.save(appointment);
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
