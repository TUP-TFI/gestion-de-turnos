package com.grupo140.turnos.settings;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Acceso a {@link PlatformSettings}. La tabla tiene una sola fila ({@code id = 1}), que inserta la migración
 * V1: solo se lee y se actualiza, nunca se crea ni se borra.
 */
public interface PlatformSettingsRepository extends Repository<PlatformSettings, Short> {

    Optional<PlatformSettings> findById(Short id);

    PlatformSettings save(PlatformSettings settings);
}
