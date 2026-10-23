package com.grupo140.turnos.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Franja de atención tal como la devuelve la API. Coincide con el tipo {@code BusinessHours} del frontend
 * ({@code frontend/src/types/BusinessHours.ts}).
 */
public record BusinessHoursResponse(
        UUID id,
        UUID companyId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer intervalMinutes,
        Instant createdAt,
        Instant updatedAt) {

    static BusinessHoursResponse from(BusinessHours hours) {
        return new BusinessHoursResponse(
                hours.getId(),
                hours.getCompanyId(),
                hours.getDayOfWeek(),
                hours.getStartTime(),
                hours.getEndTime(),
                hours.getIntervalMinutes(),
                hours.getCreatedAt(),
                hours.getUpdatedAt());
    }
}
