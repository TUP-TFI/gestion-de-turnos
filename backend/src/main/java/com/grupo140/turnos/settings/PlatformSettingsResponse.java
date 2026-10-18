package com.grupo140.turnos.settings;

/**
 * Parámetros globales tal como los devuelve la API (US-04.9). También es lo que leen los otros features,
 * por ejemplo el cálculo de disponibilidad (US-05.5).
 */
public record PlatformSettingsResponse(
        int minBookingNoticeMinutes, int cancellationDeadlineHours, int maxRescheduleCount, int maxBookingAdvanceDays) {

    static PlatformSettingsResponse from(PlatformSettings settings) {
        return new PlatformSettingsResponse(
                settings.getMinBookingNoticeMinutes(),
                settings.getCancellationDeadlineHours(),
                settings.getMaxRescheduleCount(),
                settings.getMaxBookingAdvanceDays());
    }
}
