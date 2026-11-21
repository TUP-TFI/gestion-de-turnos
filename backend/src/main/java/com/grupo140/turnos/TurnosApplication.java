package com.grupo140.turnos;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TurnosApplication {

    public static void main(String[] args) {
        // El proceso corre en UTC en cualquier máquina, igual que en Render. Con hibernate.jdbc.time_zone=UTC,
        // Hibernate pasa los LocalTime de la zona del proceso a UTC al guardarlos: en una compu en -03, la franja de
        // 00:00 a 23:59 se guardaba de 03:00 a 02:59 y la base la rechazaba (chk_hours_range). Las horas de las
        // franjas son locales de la empresa y se guardan tal cual.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(TurnosApplication.class, args);
    }
}
