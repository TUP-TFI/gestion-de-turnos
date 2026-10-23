package com.grupo140.turnos.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a {@link BusinessHours}. Extiende {@link Repository} y no {@code JpaRepository}: toda lectura lleva
 * {@code companyId} obligatorio (regla de aislamiento del README).
 */
public interface BusinessHoursRepository extends Repository<BusinessHours, UUID> {

    BusinessHours save(BusinessHours businessHours);

    // Ejecuta los INSERT pendientes en el momento: recién ahí quedan cargados createdAt y updatedAt.
    void flush();

    List<BusinessHours> findAllByCompanyId(UUID companyId);

    // Borrado masivo con @Query y no un deleteBy... derivado: Hibernate ejecuta los INSERT antes que los
    // DELETE al hacer flush, y las franjas nuevas chocarían con las viejas en excl_hours_overlap. Este
    // DELETE se ejecuta en el momento.
    @Modifying
    @Query("delete from BusinessHours b where b.companyId = :companyId")
    void deleteAllByCompanyId(@Param("companyId") UUID companyId);
}
