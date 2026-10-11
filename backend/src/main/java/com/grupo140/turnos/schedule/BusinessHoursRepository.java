package com.grupo140.turnos.schedule;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link BusinessHours}. Extiende {@link Repository} y no {@code JpaRepository}: las lecturas se
 * agregan con {@code companyId} obligatorio cuando se haga la agenda (regla de aislamiento del README).
 */
public interface BusinessHoursRepository extends Repository<BusinessHours, UUID> {

    BusinessHours save(BusinessHours businessHours);
}
