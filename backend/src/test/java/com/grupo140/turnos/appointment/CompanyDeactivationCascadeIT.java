package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelación en cascada al desactivar una empresa (US-04.4), sobre Barbería Central del seed.
 *
 * <p>Los turnos se guardan directo con el repositorio, como en {@code FutureAppointmentsIT}: hacen falta fechas
 * pasadas y estados que la reserva no permite. Va en este paquete porque el constructor de {@link Appointment} no es
 * público. Cada test hace rollback ({@code @Transactional}), salvo el de los avisos, que necesita un commit real.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CompanyDeactivationCascadeIT {

    private static final int DURATION_MINUTES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deactivationCancelsPendingAppointmentsAndKeepsTheRest() throws Exception {
        // Empresa y servicio del seed, leídos con el admin antes de desactivar (después no puede entrar).
        String services = mockMvc.perform(
                        get("/api/company/services").header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> serviceIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        UUID serviceId = UUID.fromString(serviceIds.getFirst());
        UUID companyId = UUID.fromString(companyIds.getFirst());
        UUID clientId = userRepository
                .findByEmailAndCompanyId("cliente1@example.com", companyId)
                .orElseThrow()
                .getId();

        Instant now = Instant.now();
        // Se cancelan: uno cargado a mano y uno de un cliente registrado, los dos pendientes.
        Appointment manual = save(companyId, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING, null);
        Appointment online =
                save(companyId, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.PENDING, clientId);
        // No se toca: ya estaba completado.
        Appointment completed =
                save(companyId, serviceId, now.minus(Duration.ofDays(2)), AppointmentStatus.COMPLETED, null);

        mockMvc.perform(post("/api/superadmin/companies/" + companyId + "/deactivate")
                        .header("Authorization", "Bearer " + tokenOf("superadmin@example.com")))
                .andExpect(status().isOk());

        assertThat(appointmentRepository.findByCompanyIdAndStatus(companyId, AppointmentStatus.CANCELLED))
                .extracting(Appointment::getId)
                .containsExactlyInAnyOrder(manual.getId(), online.getId());
        assertThat(manual.getCancelledBy()).isEqualTo(CancelledBy.COMPANY);
        assertThat(manual.getCancelledAt()).isNotNull();
        assertThat(manual.getCancellationReason()).isEqualTo(AppointmentService.COMPANY_DEACTIVATED_REASON);
        assertThat(appointmentRepository.findByCompanyIdAndStatus(companyId, AppointmentStatus.PENDING))
                .isEmpty();
        assertThat(completed.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    // Sin rollback: el aviso se escribe después del commit (US-07.5), así que este test limpia lo que deja.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deactivationWritesOneNotificationPerCancelledAppointmentAfterCommit() throws Exception {
        String services = mockMvc.perform(
                        get("/api/company/services").header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> serviceIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].id");
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");
        UUID serviceId = UUID.fromString(serviceIds.getFirst());
        UUID companyId = UUID.fromString(companyIds.getFirst());
        UUID clientId = userRepository
                .findByEmailAndCompanyId("cliente1@example.com", companyId)
                .orElseThrow()
                .getId();
        String superadminToken = tokenOf("superadmin@example.com");

        try {
            Instant now = Instant.now();
            UUID manual = save(companyId, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING, null)
                    .getId();
            UUID online = save(companyId, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.PENDING, clientId)
                    .getId();

            mockMvc.perform(post("/api/superadmin/companies/" + companyId + "/deactivate")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());

            assertThat(notificationOf(manual))
                    .containsEntry("event_type", "APPOINTMENT_CANCELLED")
                    .containsEntry("status", "SENT")
                    .containsEntry("recipient_phone", "+5493510000123")
                    .containsEntry("user_id", null);
            assertThat(notificationOf(online))
                    .containsEntry("event_type", "APPOINTMENT_CANCELLED")
                    .containsEntry("status", "SENT")
                    .containsEntry("user_id", clientId);
        } finally {
            mockMvc.perform(post("/api/superadmin/companies/" + companyId + "/reactivate")
                            .header("Authorization", "Bearer " + superadminToken))
                    .andExpect(status().isOk());
            jdbcTemplate.update(
                    "delete from notification"
                            + " where appointment_id in (select id from appointment where company_id = ?)",
                    companyId);
            jdbcTemplate.update("delete from appointment where company_id = ?", companyId);
        }
    }

    private Map<String, Object> notificationOf(UUID appointmentId) {
        return jdbcTemplate.queryForMap(
                "select event_type, status, recipient_phone, user_id from notification where appointment_id = ?",
                appointmentId);
    }

    // Con clientId, el turno es de un cliente registrado; sin él, es un turno cargado a mano con nombre y teléfono.
    private Appointment save(UUID companyId, UUID serviceId, Instant start, AppointmentStatus status, UUID clientId) {
        Appointment appointment = new Appointment();
        appointment.setCompanyId(companyId);
        appointment.setServiceId(serviceId);
        if (clientId != null) {
            appointment.setClientId(clientId);
            appointment.setOrigin(AppointmentOrigin.ONLINE);
        } else {
            appointment.setManualClientName("Cliente de prueba");
            appointment.setManualClientPhone("+5493510000123");
            appointment.setOrigin(AppointmentOrigin.MANUAL);
        }
        appointment.setPriceSnapshot(new BigDecimal("8000.00"));
        appointment.setDurationMinutesSnapshot(DURATION_MINUTES);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(start.plus(Duration.ofMinutes(DURATION_MINUTES)));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    private String tokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
