package com.grupo140.turnos.notification;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registra y despacha cada aviso de un turno (US-07.5), en dos fases.
 *
 * <p>La fila en {@code PENDING} se escribe dentro de la transacción del turno: si el proceso se cae después del
 * commit, la fila queda para el reintento de US-10.4. A cambio, un fallo al guardarla revierte la reserva o la
 * cancelación en cascada. Si esa transacción se revierte, no queda fila ni se envía nada.
 *
 * <p>El envío corre después del commit, en una transacción propia ({@code REQUIRES_NEW}): un envío lento no retiene
 * el lock de la empresa, y si falla, la fila queda en {@code FAILED} y el turno no cambia.
 *
 * <p>Pendiente para US-10.4: el mensaje no se guarda en la fila, así que el reintento lo tiene que rearmar con el
 * turno y el {@code eventType}.
 */
@Component
public class NotificationRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationRequestedListener.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public NotificationRequestedListener(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            ApplicationEventPublisher events,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.events = events;
        this.clock = clock;
    }

    /** Primera fase: guarda la fila en la transacción del turno. {@code MANDATORY} exige que exista una. */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        Notification notification = notificationRepository.saveAndFlush(new Notification(
                event.appointmentId(),
                event.userId(),
                event.recipientPhone(),
                event.eventType(),
                NotificationChannel.WHATSAPP));
        events.publishEvent(
                new NotificationPendingEvent(notification.getId(), event.recipientPhone(), event.message()));
    }

    /** Segunda fase: despacha el aviso, ya confirmada la transacción del turno. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationPending(NotificationPendingEvent event) {
        Notification notification =
                notificationRepository.findById(event.notificationId()).orElseThrow();
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        try {
            notificationService.dispatch(event.recipientPhone(), event.message());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now(clock));
        } catch (RuntimeException ex) {
            log.warn("No se pudo enviar el aviso {}", event.notificationId(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }
    }
}
