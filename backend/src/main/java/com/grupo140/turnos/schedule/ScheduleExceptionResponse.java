package com.grupo140.turnos.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Excepción de calendario tal como la devuelve la API. Coincide con el tipo {@code ScheduleException} del
 * frontend ({@code frontend/src/types/ScheduleException.ts}).
 */
public record ScheduleExceptionResponse(
        UUID id,
        UUID companyId,
        LocalDate exceptionDate,
        boolean isClosed,
        LocalTime startTime,
        LocalTime endTime,
        Integer intervalMinutes,
        String reason,
        Instant createdAt,
        Instant updatedAt) {

    static ScheduleExceptionResponse from(ScheduleException exception) {
        return new ScheduleExceptionResponse(
                exception.getId(),
                exception.getCompanyId(),
                exception.getExceptionDate(),
                exception.isClosed(),
                exception.getStartTime(),
                exception.getEndTime(),
                exception.getIntervalMinutes(),
                exception.getReason(),
                exception.getCreatedAt(),
                exception.getUpdatedAt());
    }
}
