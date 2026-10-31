package com.grupo140.turnos.appointment.job;

import com.grupo140.turnos.appointment.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cierra los turnos que ya terminaron (US-11.1). Corre cada 5 minutos; entre dos corridas, un turno vencido puede
 * seguir en {@code PENDING}. Se apaga con {@code app.jobs.enabled=false} (ver {@code SchedulingConfig}).
 */
@Component
public class CompleteAppointmentsJob {

    private static final Logger log = LoggerFactory.getLogger(CompleteAppointmentsJob.class);

    private final AppointmentService appointmentService;

    public CompleteAppointmentsJob(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Scheduled(cron = "${app.jobs.complete-appointments.cron:0 */5 * * * *}")
    public void completeFinishedAppointments() {
        int completed = appointmentService.completeFinished();
        // Sin turnos para cerrar no escribe nada, para no llenar el log con una línea cada 5 minutos.
        if (completed > 0) {
            log.info("Turnos completados por el job de cierre: {}", completed);
        }
    }
}
