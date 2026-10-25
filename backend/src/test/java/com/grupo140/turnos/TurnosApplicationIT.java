package com.grupo140.turnos;

import com.grupo140.turnos.seed.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Levanta el contexto completo contra un PostgreSQL real: Flyway aplica todas las migraciones
 * sobre una base limpia y Hibernate las valida contra las entidades (ddl-auto: validate).
 *
 * <p>Activa el seed de demo para verificar que sus datos respetan las constraints de la base.
 */
@SpringBootTest(properties = "app.seed.enabled=true")
@Import(TestcontainersConfiguration.class)
class TurnosApplicationIT {

    @Autowired
    private DemoDataSeeder seeder;

    @Test
    void contextLoads() {}

    // Render vuelve a correr el seed en cada deploy: sobre una base ya cargada no tiene que crear nada,
    // y un duplicado chocaría con los índices únicos de slug y email.
    @Test
    void seedRunsAgainOnLoadedDatabase() {
        seeder.run();
    }
}
