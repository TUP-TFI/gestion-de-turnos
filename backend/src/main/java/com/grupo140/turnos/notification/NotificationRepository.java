package com.grupo140.turnos.notification;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link Notification}. La tabla no tiene {@code company_id}: cada fila cuelga de un turno o de un usuario.
 */
public interface NotificationRepository extends Repository<Notification, UUID> {

    // Guarda y ejecuta el INSERT en el momento: un fallo salta dentro de la transacción del turno.
    Notification saveAndFlush(Notification notification);

    Optional<Notification> findById(UUID id);
}
