package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Turnos de una empresa en un estado, sin filtrar por fecha.
    List<Appointment> findByCompanyIdAndStatus(UUID companyId, AppointmentStatus status);

    // Agenda del admin (US-09.2). Un filtro nulo no filtra. Ordena del más nuevo al más viejo, y el id desempata para
    // que dos turnos a la misma hora no salten de una página a otra.
    @Query("""
            select a from Appointment a
            where a.companyId = :companyId
              and (:status is null or a.status = :status)
              and (:origin is null or a.origin = :origin)
              and (:rescheduled is null
                   or (:rescheduled = true and a.rescheduleCount > 0)
                   or (:rescheduled = false and a.rescheduleCount = 0))
            order by a.startDateTime desc, a.id
            """)
    Page<Appointment> searchForCompany(
            @Param("companyId") UUID companyId,
            @Param("status") AppointmentStatus status,
            @Param("origin") AppointmentOrigin origin,
            @Param("rescheduled") Boolean rescheduled,
            Pageable pageable);

    // Resumen del dashboard (US-09.1): los turnos no cancelados que empiezan en el rango [from, to), del más cercano al
    // más lejano.
    @Query("""
            select a from Appointment a
            where a.companyId = :companyId
              and a.status <> com.grupo140.turnos.appointment.AppointmentStatus.CANCELLED
              and a.startDateTime >= :from
              and a.startDateTime < :to
            order by a.startDateTime, a.id
            """)
    List<Appointment> findNotCancelledBetween(
            @Param("companyId") UUID companyId, @Param("from") Instant from, @Param("to") Instant to);

    // Los próximos turnos del resumen (D4 de contratos-api.md): los 5 primeros no cancelados desde un instante.
    List<Appointment> findFirst5ByCompanyIdAndStatusNotAndStartDateTimeGreaterThanEqualOrderByStartDateTimeAscIdAsc(
            UUID companyId, AppointmentStatus status, Instant from);

    // Lista de avisos de la desactivación (US-04.4): los cancelados con el motivo de la baja que todavía no pasaron.
    List<Appointment> findAllByCompanyIdAndStatusAndCancellationReasonAndStartDateTimeAfterOrderByStartDateTimeAsc(
            UUID companyId, AppointmentStatus status, String cancellationReason, Instant after);

    // "Mis turnos" del cliente (US-07.6), del más reciente al más antiguo. Llevan los dos ids: el cliente y su
    // empresa. Usan idx_appointment_client_start.
    List<Appointment> findAllByCompanyIdAndClientIdOrderByStartDateTimeDesc(UUID companyId, UUID clientId);

    List<Appointment> findAllByCompanyIdAndClientIdAndStatusOrderByStartDateTimeDesc(
            UUID companyId, UUID clientId, AppointmentStatus status);

    // Un turno del cliente (US-08.1). Lleva los tres ids: el turno de otro cliente o de otra empresa no aparece.
    Optional<Appointment> findByIdAndCompanyIdAndClientId(UUID id, UUID companyId, UUID clientId);

    // Un turno de la empresa, de cualquier cliente o sin cuenta (US-09.3). El de otra empresa no aparece.
    Optional<Appointment> findByIdAndCompanyId(UUID id, UUID companyId);

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
