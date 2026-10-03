package com.grupo140.turnos.notification;

/**
 * Canales de envío de una {@link Notification}. Persistido como {@code varchar} + {@code CHECK} (ver
 * esquema-bd.md).
 */
public enum NotificationChannel {
    WHATSAPP,
    EMAIL
}
