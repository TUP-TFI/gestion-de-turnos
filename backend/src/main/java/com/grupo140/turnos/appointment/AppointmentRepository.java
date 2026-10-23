package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.util.List;
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

    List<Appointment> findAllByCompanyIdAndStatusAndStartDateTimeAfter(
            UUID companyId, AppointmentStatus status, Instant after);

    // Turnos de una empresa en un estado. La desactivación de la empresa cancela todos los PENDING (US-04.4).
    List<Appointment> findByCompanyIdAndStatus(UUID companyId, AppointmentStatus status);

    // Dashboard del superadmin (US-04.6): total de turnos de toda la plataforma, en cualquier estado. No
    // lleva companyId porque no devuelve datos de una empresa, solo un número agregado.
    long count();
}
