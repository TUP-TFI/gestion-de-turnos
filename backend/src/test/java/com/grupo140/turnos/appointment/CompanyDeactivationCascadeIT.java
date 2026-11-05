package com.grupo140.turnos.appointment;

import static com.grupo140.turnos.seed.SeedTestValues.DEV_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grupo140.turnos.TestcontainersConfiguration;
import com.grupo140.turnos.user.UserRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancelación en cascada al desactivar una empresa (US-04.4), sobre Barbería Central del seed.
 *
 * <p>Los turnos se guardan directo con el repositorio, como en {@code FutureAppointmentsIT}: hacen falta fechas
 * pasadas y estados que la reserva no permite. Va en este paquete porque el constructor de {@link Appointment} no es
 * público. Cada test hace rollback ({@code @Transactional}).
 *
 * <p>También cubre la lista de avisos de la desactivación, que el superadmin usa para mandar cada WhatsApp.
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

    // El sistema no avisa: devuelve la lista para que el superadmin mande cada WhatsApp. Solo trae los turnos que
    // cancela la baja y todavía no pasaron, ordenados por fecha, con el contacto de la cuenta o del propio turno.
    @Test
    void deactivationListsFutureCancelledAppointmentsForNotices() throws Exception {
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

        Instant now = Instant.now();
        Appointment online =
                save(companyId, serviceId, now.plus(Duration.ofDays(3)), AppointmentStatus.PENDING, clientId);
        Appointment manual = save(companyId, serviceId, now.plus(Duration.ofDays(2)), AppointmentStatus.PENDING, null);
        // Quedan afuera: uno pendiente que ya pasó, y uno que el cliente había cancelado antes de la baja.
        save(companyId, serviceId, now.minus(Duration.ofDays(1)), AppointmentStatus.PENDING, null);
        Appointment cancelledByClient =
                save(companyId, serviceId, now.plus(Duration.ofDays(4)), AppointmentStatus.PENDING, clientId);
        // Se guarda PENDING y después se cancela: un INSERT en CANCELLED, sin cancelled_by ni cancelled_at, viola
        // chk_appointment_cancellation.
        cancelledByClient.setStatus(AppointmentStatus.CANCELLED);
        cancelledByClient.setCancelledBy(CancelledBy.CLIENT);
        cancelledByClient.setCancelledAt(now);
        appointmentRepository.save(cancelledByClient);

        mockMvc.perform(post("/api/superadmin/companies/" + companyId + "/deactivate")
                        .header("Authorization", "Bearer " + superadminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/superadmin/companies/" + companyId + "/cancellation-notices")
                        .header("Authorization", "Bearer " + superadminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Argentina/Cordoba"))
                .andExpect(jsonPath("$.notices.length()").value(2))
                .andExpect(jsonPath("$.notices[0].appointmentId")
                        .value(manual.getId().toString()))
                .andExpect(jsonPath("$.notices[0].clientName").value("Cliente de prueba"))
                .andExpect(jsonPath("$.notices[0].phone").value("+5493510000123"))
                .andExpect(jsonPath("$.notices[0].serviceName").value("Corte de pelo"))
                .andExpect(jsonPath("$.notices[0].startDateTime").isNotEmpty())
                .andExpect(jsonPath("$.notices[1].appointmentId")
                        .value(online.getId().toString()))
                .andExpect(jsonPath("$.notices[1].clientName").value("Lucía Pérez"))
                .andExpect(jsonPath("$.notices[1].phone").value("+5493510000004"));

        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from notification where appointment_id in (?, ?)",
                        Long.class,
                        manual.getId(),
                        online.getId()))
                .isZero();
    }

    @Test
    void cancellationNoticesAreOnlyForSuperadmin() throws Exception {
        String services = mockMvc.perform(
                        get("/api/company/services").header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> companyIds = JsonPath.read(services, "$[?(@.name == 'Corte de pelo')].companyId");

        mockMvc.perform(get("/api/superadmin/companies/" + companyIds.getFirst() + "/cancellation-notices")
                        .header("Authorization", "Bearer " + tokenOf("admin@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellationNoticesOfUnknownCompanyReturn404() throws Exception {
        mockMvc.perform(get("/api/superadmin/companies/" + UUID.randomUUID() + "/cancellation-notices")
                        .header("Authorization", "Bearer " + tokenOf("superadmin@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
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
