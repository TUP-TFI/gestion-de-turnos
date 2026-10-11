package com.grupo140.turnos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Levanta el contexto completo contra un PostgreSQL real: Flyway aplica todas las migraciones
 * sobre una base limpia y Hibernate las valida contra las entidades (ddl-auto: validate).
 *
 * <p>Activa el seed de desarrollo para verificar que sus datos respetan las constraints de la base.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@Import(TestcontainersConfiguration.class)
class TurnosApplicationIT {

    @Test
    void contextLoads() {}
}
