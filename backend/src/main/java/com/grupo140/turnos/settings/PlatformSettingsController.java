package com.grupo140.turnos.settings;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parámetros globales del panel del superadmin (US-04.9). El prefijo {@code /api/superadmin} ya exige el rol
 * {@code SUPERADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/superadmin/settings")
public class PlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;

    public PlatformSettingsController(PlatformSettingsService platformSettingsService) {
        this.platformSettingsService = platformSettingsService;
    }

    /** Los valores vigentes, para cargar el formulario. */
    @GetMapping
    public PlatformSettingsResponse get() {
        return platformSettingsService.get();
    }

    /** Reemplaza los cuatro valores. */
    @PutMapping
    public PlatformSettingsResponse update(@Valid @RequestBody PlatformSettingsRequest request) {
        return platformSettingsService.update(request);
    }
}
