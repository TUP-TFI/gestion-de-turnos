package com.grupo140.turnos.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.grupo140.turnos.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de demo para la presentación (T-14.2), con {@code app.seed.demo-data=true}. Esta configuración levanta su
 * propia base, así que estos turnos no llegan a los demás tests, que usan el seed sin turnos. Las consultas van con SQL
 * directo: miran lo que quedó guardado, sin pasar por los services.
 */
@SpringBootTest(properties = {"app.seed.enabled=true", "app.seed.demo-data=true"})
@Import(TestcontainersConfiguration.class)
class PresentationDataSeedIT {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void barberiaHasAppointmentsInEveryState() {
        assertThat(countBarberia("status = 'PENDING' and start_date_time > now()"))
                .isPositive();
        assertThat(countBarberia("status = 'PENDING' and reschedule_count > 0")).isPositive();
        assertThat(countBarberia("status = 'COMPLETED'")).isPositive();
        assertThat(countBarberia("status = 'CANCELLED' and cancelled_by = 'CLIENT'"))
                .isPositive();
        assertThat(countBarberia(
                        "status = 'CANCELLED' and cancelled_by = 'COMPANY' and cancellation_reason is not null"))
                .isPositive();
        assertThat(countBarberia("origin = 'MANUAL' and client_id is not null")).isPositive();
        assertThat(countBarberia("origin = 'MANUAL' and client_id is null")).isPositive();
    }

    @Test
    void esteticaLuminaIsAnActiveCompanyWithItsOwnData() {
        assertThat(jdbcTemplate.queryForObject(
                        "select status from company where slug = 'estetica-lumina'", String.class))
                .isEqualTo("ACTIVE");
        assertThat(countEstetica("select count(*) from service where company_id = ?"))
                .isEqualTo(3);
        assertThat(countEstetica("select count(*) from business_hours where company_id = ?"))
                .isEqualTo(5);
        assertThat(
                        countEstetica(
                                "select count(*) from app_user where company_id = ? and role = 'COMPANY_ADMIN' and status = 'ACTIVE'"))
                .isEqualTo(1);
        assertThat(countEstetica("select count(*) from app_user where company_id = ? and role = 'CLIENT'"))
                .isEqualTo(2);
        assertThat(countEstetica("select count(*) from appointment where company_id = ?"))
                .isEqualTo(4);
    }

    // Render corre el seed en cada deploy: sobre una base ya cargada no tiene que crear nada.
    @Test
    void runningTheSeedAgainCreatesNothing() {
        long companies = count("select count(*) from company");
        long users = count("select count(*) from app_user");
        long appointments = count("select count(*) from appointment");

        seeder.run();

        assertThat(count("select count(*) from company")).isEqualTo(companies);
        assertThat(count("select count(*) from app_user")).isEqualTo(users);
        assertThat(count("select count(*) from appointment")).isEqualTo(appointments);
    }

    private long countBarberia(String condition) {
        // Sin join: company también tiene una columna status, y la condición quedaría ambigua.
        return count("select count(*) from appointment where company_id ="
                + " (select id from company where slug = 'barberia-central') and " + condition);
    }

    private long countEstetica(String sql) {
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                jdbcTemplate.queryForObject("select id from company where slug = 'estetica-lumina'", Object.class));
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
