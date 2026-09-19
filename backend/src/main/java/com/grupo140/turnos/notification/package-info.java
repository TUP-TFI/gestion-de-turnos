/**
 * {@code NotificationService} desacoplado del canal, con reintentos.
 * <p>
 * Hoja del grafo: no depende de ningún feature de negocio. Recibe eventos de dominio
 * vía {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
package com.grupo140.turnos.notification;
