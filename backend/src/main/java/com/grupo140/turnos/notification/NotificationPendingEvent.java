package com.grupo140.turnos.notification;

import java.util.UUID;

/**
 * Evento interno de {@code notification}: la fila ya está guardada y falta despacharla. Lo publica y lo escucha
 * {@link NotificationRequestedListener}; los demás features publican {@link NotificationRequestedEvent}.
 */
public record NotificationPendingEvent(UUID notificationId, String recipientPhone, String message) {}
