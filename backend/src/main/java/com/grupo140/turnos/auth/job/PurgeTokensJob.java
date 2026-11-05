package com.grupo140.turnos.auth.job;

import com.grupo140.turnos.auth.ActivationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra los tokens vencidos y los ya usados (US-11.3). Corre una vez por día; se apaga con
 * {@code app.jobs.enabled=false} (ver {@code SchedulingConfig}).
 */
@Component
public class PurgeTokensJob {

    private static final Logger log = LoggerFactory.getLogger(PurgeTokensJob.class);

    private final ActivationService activationService;

    public PurgeTokensJob(ActivationService activationService) {
        this.activationService = activationService;
    }

    @Scheduled(cron = "${app.jobs.purge-tokens.cron:0 0 4 * * *}")
    public void purgeDeadTokens() {
        int purged = activationService.purgeDeadTokens();
        log.info("Tokens borrados por el job de purga: {}", purged);
    }
}
