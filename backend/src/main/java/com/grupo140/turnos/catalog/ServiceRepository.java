package com.grupo140.turnos.catalog;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link Service}. Extiende {@link Repository} y no {@code JpaRepository}: las lecturas se
 * agregan con {@code companyId} obligatorio cuando se haga el catálogo (regla de aislamiento del README).
 */
public interface ServiceRepository extends Repository<Service, UUID> {

    Service save(Service service);
}
