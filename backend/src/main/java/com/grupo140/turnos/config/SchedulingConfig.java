package com.grupo140.turnos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los jobs programados. Están prendidos por defecto; los tests los apagan con {@code app.jobs.enabled=false}
 * y llaman a cada método directo, sin esperar al scheduler.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.jobs.enabled", matchIfMissing = true)
public class SchedulingConfig {}
