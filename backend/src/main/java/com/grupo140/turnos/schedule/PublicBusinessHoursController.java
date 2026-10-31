package com.grupo140.turnos.schedule;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Horario de atención en la página pública de cada empresa. Va bajo {@code /api/public/**}: se ve sin login (ver
 * {@code SecurityConfig}), y la empresa sale del slug de la URL.
 */
@RestController
@RequestMapping("/api/public/companies/{slug}")
public class PublicBusinessHoursController {

    private final ScheduleService scheduleService;

    public PublicBusinessHoursController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** Días y franjas en que opera la empresa. Los turnos libres son la disponibilidad (US-06.2). */
    @GetMapping("/business-hours")
    public List<PublicBusinessHoursResponse> get(@PathVariable String slug) {
        return scheduleService.publicBusinessHours(slug);
    }
}
