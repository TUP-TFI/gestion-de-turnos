package com.grupo140.turnos.settings;

import com.grupo140.turnos.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parámetros globales de la plataforma (US-04.9), y API de {@code settings} para el resto de los features.
 *
 * <p>Se leen de la base en cada consulta, sin guardarlos en memoria: un cambio del superadmin rige de
 * inmediato, sin redeploy.
 */
@Service
public class PlatformSettingsService {

    private static final int MINUTES_PER_DAY = 1440;

    private final PlatformSettingsRepository platformSettingsRepository;

    public PlatformSettingsService(PlatformSettingsRepository platformSettingsRepository) {
        this.platformSettingsRepository = platformSettingsRepository;
    }

    /** Los valores vigentes. */
    @Transactional(readOnly = true)
    public PlatformSettingsResponse get() {
        return PlatformSettingsResponse.from(findSettings());
    }

    /**
     * Reemplaza los cuatro valores. No toca los turnos ya reservados: solo aplican a las operaciones
     * posteriores.
     *
     * @throws ApiException {@code VALIDATION_ERROR} (400) si la anticipación mínima supera a la máxima: no
     *     quedaría ningún horario reservable.
     */
    @Transactional
    public PlatformSettingsResponse update(PlatformSettingsRequest request) {
        // Que sean iguales es válido: la ventana queda reducida a un instante, pero no vacía.
        if (request.minBookingNoticeMinutes() > request.maxBookingAdvanceDays() * MINUTES_PER_DAY) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La anticipación mínima no puede superar a la máxima");
        }
        PlatformSettings settings = findSettings();
        settings.setMinBookingNoticeMinutes(request.minBookingNoticeMinutes());
        settings.setCancellationDeadlineHours(request.cancellationDeadlineHours());
        settings.setMaxRescheduleCount(request.maxRescheduleCount());
        settings.setMaxBookingAdvanceDays(request.maxBookingAdvanceDays());
        return PlatformSettingsResponse.from(platformSettingsRepository.save(settings));
    }

    // La fila la inserta la migración V1: si falta, la base está mal armada y no es un error del pedido.
    private PlatformSettings findSettings() {
        return platformSettingsRepository
                .findById(PlatformSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de platform_settings"));
    }
}
