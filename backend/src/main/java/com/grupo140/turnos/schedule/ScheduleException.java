package com.grupo140.turnos.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Fecha puntual en la que una empresa no sigue su {@link BusinessHours}: está cerrada (feriado,
 * vacaciones) o abre con un horario especial. Reemplaza por completo al horario habitual de esa fecha.
 * Mapea la tabla física {@code schedule_exception} (ver esquema-bd.md).
 *
 * <p>{@code companyId} se mantiene como UUID simple (no como relación JPA hacia {@code Company}): la
 * regla de capas del README evita que un feature importe directamente el modelo de otro.
 */
@Entity
@Table(name = "schedule_exception")
public class ScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = true;

    // Los tres siguientes son nulos si closed = true y obligatorios si closed = false (CHECK en la base).
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "interval_minutes")
    private Integer intervalMinutes;

    @Column
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduleException() {
        // JPA
    }

    // Día cerrado.
    public ScheduleException(UUID companyId, LocalDate exceptionDate) {
        this.companyId = companyId;
        this.exceptionDate = exceptionDate;
    }

    // Día abierto con horario especial.
    public ScheduleException(
            UUID companyId, LocalDate exceptionDate, LocalTime startTime, LocalTime endTime, Integer intervalMinutes) {
        this.companyId = companyId;
        this.exceptionDate = exceptionDate;
        this.closed = false;
        this.startTime = startTime;
        this.endTime = endTime;
        this.intervalMinutes = intervalMinutes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public LocalDate getExceptionDate() {
        return exceptionDate;
    }

    public void setExceptionDate(LocalDate exceptionDate) {
        this.exceptionDate = exceptionDate;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
