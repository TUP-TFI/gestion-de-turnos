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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conteo de turnos futuros de un servicio (US-05.2) con turnos reales en la base, sobre la empresa del seed.
 *
 * <p>Todavía no existe la reserva (Paso 7), así que los turnos se guardan directo con el repositorio. Cada test
 * hace rollback ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FutureAppointmentsIT {

    private static final String SERVICES = "/api/company/services";
    private static final int DURATION_MINUTES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void countsOnlyFuturePendingAppointments() throws Exception {
        String token = adminToken();
        String services = mockMvc.perform(get(SERVICES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> ids = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        UUID serviceId = UUID.fromString(ids.getFirst());
        UUID companyId = UUID.fromString(companyIds.getFirst());

        Instant now = Instant.now();
        // Cuenta: pendiente y futuro.
        save(companyId, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        // No cuentan: uno ya pasado y uno futuro pero cancelado.
        save(companyId, serviceId, now.minus(Duration.ofDays(2)), AppointmentStatus.PENDING);
        save(companyId, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.CANCELLED);

        mockMvc.perform(get(SERVICES + "/" + serviceId + "/future-appointments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    // Turno manual (sin cuenta), para no depender de los clientes del seed.
    private void save(UUID companyId, UUID serviceId, Instant start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
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
