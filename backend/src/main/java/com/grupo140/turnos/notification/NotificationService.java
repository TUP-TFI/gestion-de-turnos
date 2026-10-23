package com.grupo140.turnos.notification;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Punto único de envío de avisos a los usuarios.
 *
 * <p>Hasta que exista el envío por WhatsApp (Paso 10) no manda nada: escribe el mensaje en el log para
 * poder copiarlo a mano.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Link de activación del admin de una empresa recién creada (US-04.1). */
    public void sendActivationLink(String phone, String link) {
        // Temporal: en el Paso 10 se reemplaza por el envío real y el link deja de escribirse en el log.
        log.info("Link de activación para {}: {}", phone, link);
    }

    /** Aviso al cliente de que su turno se canceló, con el motivo (US-04.4). */
    public void sendAppointmentCancelled(String phone, Instant startDateTime, String reason) {
        // Temporal: en el Paso 10 se reemplaza por el envío real.
        log.info("Aviso para {}: el turno del {} fue cancelado. Motivo: {}", phone, startDateTime, reason);
    }
}
