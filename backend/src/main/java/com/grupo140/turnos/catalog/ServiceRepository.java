package com.grupo140.turnos.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link Service}. Extiende {@link Repository} y no {@code JpaRepository}: toda lectura lleva
 * {@code companyId} obligatorio (regla de aislamiento del README).
 */
public interface ServiceRepository extends Repository<Service, UUID> {

    Service save(Service service);

    // Guarda y ejecuta el INSERT en el momento, sin esperar al final de la transacción: recién ahí
    // quedan cargados createdAt y updatedAt.
    Service saveAndFlush(Service service);

    List<Service> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<Service> findByIdAndCompanyId(UUID id, UUID companyId);
}
