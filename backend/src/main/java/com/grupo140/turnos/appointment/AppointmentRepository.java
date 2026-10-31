package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a {@link Appointment}. Extiende {@link Repository} y no {@code JpaRepository}: toda lectura
 * lleva {@code companyId} obligatorio (regla de aislamiento del README).
 */
public interface AppointmentRepository extends Repository<Appointment, UUID> {

    Appointment save(Appointment appointment);

    // Guarda y ejecuta el INSERT en el momento: la constraint excl_appointment_overlap salta dentro de la
    // transacción de la reserva (US-07.3), no al hacer commit.
    Appointment saveAndFlush(Appointment appointment);

    long countByCompanyIdAndServiceIdAndStatusAndStartDateTimeAfter(
            UUID companyId, UUID serviceId, AppointmentStatus status, Instant after);

    List<Appointment> findAllByCompanyIdAndStatusAndStartDateTimeAfter(
            UUID companyId, AppointmentStatus status, Instant after);

    // Turnos de una empresa en un estado. La desactivación de la empresa cancela todos los PENDING (US-04.4).
    List<Appointment> findByCompanyIdAndStatus(UUID companyId, AppointmentStatus status);

    // "Mis turnos" del cliente (US-07.6), del más reciente al más antiguo. Llevan los dos ids: el cliente y su
    // empresa. Usan idx_appointment_client_start.
    List<Appointment> findAllByCompanyIdAndClientIdOrderByStartDateTimeDesc(UUID companyId, UUID clientId);

    List<Appointment> findAllByCompanyIdAndClientIdAndStatusOrderByStartDateTimeDesc(
            UUID companyId, UUID clientId, AppointmentStatus status);

    // Turnos de la empresa que ocupan algún tramo entre dos instantes, sin los de un estado (CANCELLED). Un turno
    // se superpone con el rango si empieza antes de su fin y termina después de su inicio: el mismo criterio que
    // la constraint excl_appointment_overlap, que cuenta PENDING y COMPLETED. La usa la disponibilidad (US-05.5).
    List<Appointment> findAllByCompanyIdAndStatusNotAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
            UUID companyId, AppointmentStatus status, Instant rangeEnd, Instant rangeStart);

    // Dashboard del superadmin (US-04.6): total de turnos de toda la plataforma, en cualquier estado. No
    // lleva companyId porque no devuelve datos de una empresa, solo un número agregado.
    long count();

    // Job de cierre (US-11.1): recorre toda la plataforma, por eso no lleva companyId. El corte es endDateTime, no
    // startDateTime. Un update masivo no dispara @UpdateTimestamp, así que updatedAt se escribe acá. Devuelve
    // cuántos turnos cambió. El índice parcial idx_appointment_pending_start, sobre start_date_time, solo reduce la
    // búsqueda a los PENDING: el corte por endDateTime se filtra sobre esas filas.
    @Modifying
    @Query("update Appointment a set a.status = com.grupo140.turnos.appointment.AppointmentStatus.COMPLETED,"
            + " a.updatedAt = :now"
            + " where a.status = com.grupo140.turnos.appointment.AppointmentStatus.PENDING"
            + " and a.endDateTime <= :now")
    int completeFinished(@Param("now") Instant now);
}
