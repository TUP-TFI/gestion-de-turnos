package com.grupo140.turnos.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link ScheduleException}. Extiende {@link Repository} y no {@code JpaRepository}: toda lectura
 * lleva {@code companyId} obligatorio (regla de aislamiento del README).
 */
public interface ScheduleExceptionRepository extends Repository<ScheduleException, UUID> {

    // Guarda y ejecuta el INSERT o el UPDATE en el momento, sin esperar al final de la transacción: recién
    // ahí quedan cargados createdAt y updatedAt.
    ScheduleException saveAndFlush(ScheduleException exception);

    void delete(ScheduleException exception);

    List<ScheduleException> findAllByCompanyIdAndExceptionDateGreaterThanEqualOrderByExceptionDateAsc(
            UUID companyId, LocalDate from);

    Optional<ScheduleException> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndExceptionDate(UUID companyId, LocalDate exceptionDate);

    boolean existsByCompanyIdAndExceptionDateAndIdNot(UUID companyId, LocalDate exceptionDate, UUID id);
}
