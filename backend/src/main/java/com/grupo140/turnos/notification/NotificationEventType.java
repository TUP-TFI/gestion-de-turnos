package com.grupo140.turnos.notification;

/**
 * Eventos que disparan una {@link Notification}. Persistido como {@code varchar} + {@code CHECK} (ver
 * esquema-bd.md).
 */
public enum NotificationEventType {
    BOOKING_CONFIRMED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_RESCHEDULED,
    ACCOUNT_ACTIVATION,
    PASSWORD_RESET
}
