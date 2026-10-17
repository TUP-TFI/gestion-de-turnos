package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link Appointment}. Extiende {@link Repository} y no {@code JpaRepository}: toda lectura
 * lleva {@code companyId} obligatorio (regla de aislamiento del README).
 */
public interface AppointmentRepository extends Repository<Appointment, UUID> {

    Appointment save(Appointment appointment);

    long countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(
            UUID companyId, UUID serviceId, AppointmentStatus status, Instant after);
}
