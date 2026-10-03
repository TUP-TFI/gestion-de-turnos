package com.grupo140.turnos.notification;

/**
 * Estados posibles de una {@link Notification}. Persistido como {@code varchar} + {@code CHECK} (ver
 * esquema-bd.md).
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
