package com.grupo140.turnos.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Registro de un mensaje que el sistema envió o tiene pendiente de enviar, con su estado y sus
 * reintentos. Mapea la tabla física {@code notification} (ver esquema-bd.md).
 *
 * <p>{@code appointmentId} y {@code userId} se mantienen como UUID simples (no como relaciones JPA
 * hacia {@code Appointment} y {@code User}): la regla de capas del README evita que un feature importe
 * directamente el modelo de otro.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nulo en eventos de cuenta (ACCOUNT_ACTIVATION, PASSWORD_RESET).
    @Column(name = "appointment_id")
    private UUID appointmentId;

    // Nulo si el destinatario es un cliente manual sin cuenta.
    @Column(name = "user_id")
    private UUID userId;

    // Teléfono al que se envió, copiado al crear la notificación. No cambia si el usuario edita su teléfono después.
    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    // Cargado si y solo si status = SENT (CHECK en la base).
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(
            UUID appointmentId,
            UUID userId,
            String recipientPhone,
            NotificationEventType eventType,
            NotificationChannel channel) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.recipientPhone = recipientPhone;
        this.eventType = eventType;
        this.channel = channel;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
