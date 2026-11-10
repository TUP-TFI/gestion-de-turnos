package com.grupo140.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantías del esquema que dependen de PostgreSQL y no de la capa de servicio (T-12.4): la exclusión de turnos
 * superpuestos, el email único por empresa y las FK compuestas de {@code appointment}. Los datos se insertan con SQL
 * directo, sin pasar por los services, así que lo único que puede rechazarlos es la constraint de la base. Cada test
 * comprueba el nombre de la constraint, para que no pase por otro error. Cada test hace rollback
 * ({@code @Transactional}).
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class SchemaGuaranteesIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID companyA;
    private UUID companyB;
    private UUID serviceA;
    private UUID serviceB;
    private UUID clientA;
    private UUID clientB;
    // Un horario cualquiera en el futuro: la base no valida fechas, solo que no se superpongan.
    private final Instant start = Instant.now().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.HOURS);

    @BeforeEach
    void setUp() {
        companyA = company("garantias-a");
        companyB = company("garantias-b");
        serviceA = service(companyA);
        serviceB = service(companyB);
        clientA = client(companyA, "ana@example.com");
        clientB = client(companyB, "beto@example.com");
    }

    @Test
    void overlappingAppointmentsAreRejected() {
        appointment(companyA, serviceA, clientA, start, "PENDING");

        assertThatThrownBy(
                        () -> appointment(companyA, serviceA, clientA, start.plus(Duration.ofMinutes(15)), "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("excl_appointment_overlap");
    }

    @Test
    void cancelledAppointmentDoesNotBlockTheSlot() {
        appointment(companyA, serviceA, clientA, start, "PENDING");
        appointment(companyA, serviceA, clientA, start, "CANCELLED");

        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from appointment where company_id = ?", Integer.class, companyA))
                .isEqualTo(2);
    }

    @Test
    void sameEmailInAnotherCompanyIsAllowed() {
        client(companyB, "ana@example.com");

        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from app_user where email = 'ana@example.com'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void sameEmailInSameCompanyIsRejected() {
        assertThatThrownBy(() -> client(companyA, "ana@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_user_email_per_company");
    }

    @Test
    void emailComparisonIgnoresCase() {
        client(companyA, "Juan@mail.com");

        assertThatThrownBy(() -> client(companyA, "juan@mail.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_user_email_per_company");
    }

    @Test
    void appointmentWithServiceOfAnotherCompanyIsRejected() {
        assertThatThrownBy(() -> appointment(companyA, serviceB, clientA, start, "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_appointment_service");
    }

    @Test
    void appointmentWithClientOfAnotherCompanyIsRejected() {
        assertThatThrownBy(() -> appointment(companyA, serviceA, clientB, start, "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_appointment_client");
    }

    private UUID company(String slug) {
        return jdbcTemplate.queryForObject(
                "insert into company (name, slug, category) values (?, ?, 'OTHER') returning id",
                UUID.class,
                slug,
                slug);
    }

    private UUID service(UUID company) {
        return jdbcTemplate.queryForObject(
                "insert into service (company_id, name, price, duration_minutes) values (?, 'Corte', 8000, 30)"
                        + " returning id",
                UUID.class,
                company);
    }

    private UUID client(UUID company, String email) {
        return jdbcTemplate.queryForObject(
                "insert into app_user (first_name, last_name, email, phone, role, company_id, status)"
                        + " values ('Ana', 'Gil', ?, '+5493510000099', 'CLIENT', ?, 'ACTIVE') returning id",
                UUID.class,
                email,
                company);
    }

    // Turno de 30 minutos. Uno cancelado lleva quién y cuándo, porque lo exige chk_appointment_cancellation.
    private void appointment(UUID company, UUID service, UUID client, Instant from, String status) {
        boolean cancelled = status.equals("CANCELLED");
        jdbcTemplate.update(
                "insert into appointment (company_id, service_id, client_id, price_snapshot, duration_minutes_snapshot,"
                        + " start_date_time, end_date_time, origin, status, cancelled_by, cancelled_at)"
                        + " values (?, ?, ?, 8000, 30, ?, ?, 'ONLINE', ?, ?, ?)",
                company,
                service,
                client,
                utc(from),
                utc(from.plus(Duration.ofMinutes(30))),
                status,
                cancelled ? "CLIENT" : null,
                cancelled ? utc(from) : null);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
