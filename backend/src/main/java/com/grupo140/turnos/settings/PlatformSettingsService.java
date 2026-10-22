package com.grupo140.turnos.settings;

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
     */
    @Transactional
    public PlatformSettingsResponse update(PlatformSettingsRequest request) {
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
