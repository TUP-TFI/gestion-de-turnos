package com.grupo140.turnos.notification;

import java.util.UUID;

/**
 * Pedido de avisarle algo a un usuario sobre un turno. Vive en {@code notification} para que siga siendo hoja del
 * grafo: lo publica {@code appointment} y lo escucha {@link NotificationRequestedListener}.
 *
 * @param userId nulo si el destinatario es un cliente manual sin cuenta.
 */
public record NotificationRequestedEvent(
        UUID appointmentId, UUID userId, String recipientPhone, NotificationEventType eventType, String message) {}
