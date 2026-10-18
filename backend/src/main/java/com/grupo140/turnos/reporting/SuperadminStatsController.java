package com.grupo140.turnos.reporting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard del superadmin (US-04.6). El prefijo {@code /api/superadmin} ya exige el rol {@code SUPERADMIN}
 * (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/superadmin/stats")
public class SuperadminStatsController {

    private final SuperadminStatsService superadminStatsService;

    public SuperadminStatsController(SuperadminStatsService superadminStatsService) {
        this.superadminStatsService = superadminStatsService;
    }

    @GetMapping
    public SuperadminStatsResponse get() {
        return superadminStatsService.get();
    }
}
