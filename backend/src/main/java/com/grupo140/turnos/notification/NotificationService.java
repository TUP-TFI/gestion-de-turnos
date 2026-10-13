package com.grupo140.turnos.notification;

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
}
