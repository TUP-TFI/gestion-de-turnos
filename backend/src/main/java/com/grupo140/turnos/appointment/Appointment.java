package com.grupo140.turnos.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Registro de las citas o reservas del sistema. Es un snapshot inmutable de lo pactado, no una
 * referencia viva al catálogo (ver diccionario-datos.md). Mapea la tabla física {@code appointment}.
 *
 * <p>{@code companyId}, {@code serviceId} y {@code clientId} se mantienen como UUID simples (no
 * como relaciones JPA hacia {@code Company} y {@code Service}) porque esas entidades pertenecen a
 * features de responsabilidad de otro desarrollador: la regla de capas del README evita que un
 * feature importe directamente el modelo de otro. Las FK compuestas que garantizan a nivel de
 * motor que {@code service} y {@code client} pertenecen a la misma empresa viven en la migración
 * (V1), no en el mapeo de esta entidad.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    // Nulo en reservas manuales sin cuenta (ver manualClientName/manualClientPhone).
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "manual_client_name")
    private String manualClientName;

    @Column(name = "manual_client_phone")
    private String manualClientPhone;

    // Precio y duración pactados al momento de reservar: no cambian si se edita el catálogo.
    @Column(name = "price_snapshot", nullable = false)
    private BigDecimal priceSnapshot;

    @Column(name = "duration_minutes_snapshot", nullable = false)
    private Integer durationMinutesSnapshot;

    @Column(name = "start_date_time", nullable = false)
    private Instant startDateTime;

    // La escribe la capa de service: startDateTime + Duration.ofMinutes(durationMinutesSnapshot).
    @Column(name = "end_date_time", nullable = false)
    private Instant endDateTime;

    @Column(name = "previous_start_date_time")
    private Instant previousStartDateTime;

    @Column(name = "reschedule_count", nullable = false)
    private Integer rescheduleCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private CancelledBy cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
        // JPA
    }

    // Turno de un servicio, con su precio y su duración ya copiados. El horario, el cliente y el estado se completan
    // con
    // los setters. Lo usa el seed de demo, que está en otro paquete.
    public Appointment(UUID companyId, UUID serviceId, BigDecimal priceSnapshot, Integer durationMinutesSnapshot) {
        this.companyId = companyId;
        this.serviceId = serviceId;
        this.priceSnapshot = priceSnapshot;
        this.durationMinutesSnapshot = durationMinutesSnapshot;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getManualClientName() {
        return manualClientName;
    }

    public void setManualClientName(String manualClientName) {
        this.manualClientName = manualClientName;
    }

    public String getManualClientPhone() {
        return manualClientPhone;
    }

    public void setManualClientPhone(String manualClientPhone) {
        this.manualClientPhone = manualClientPhone;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public Integer getDurationMinutesSnapshot() {
        return durationMinutesSnapshot;
    }

    public void setDurationMinutesSnapshot(Integer durationMinutesSnapshot) {
        this.durationMinutesSnapshot = durationMinutesSnapshot;
    }

    public Instant getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Instant startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Instant getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Instant endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Instant getPreviousStartDateTime() {
        return previousStartDateTime;
    }

    public void setPreviousStartDateTime(Instant previousStartDateTime) {
        this.previousStartDateTime = previousStartDateTime;
    }

    public Integer getRescheduleCount() {
        return rescheduleCount;
    }

    public void setRescheduleCount(Integer rescheduleCount) {
        this.rescheduleCount = rescheduleCount;
    }

    public AppointmentOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(AppointmentOrigin origin) {
        this.origin = origin;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public CancelledBy getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(CancelledBy cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
