package com.grupo140.turnos.appointment;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Horarios disponibles en la página pública de cada empresa. Va bajo {@code /api/public/**}: se ven sin login
 * (ver {@code SecurityConfig}), y la empresa sale del slug de la URL.
 */
@RestController
@RequestMapping("/api/public/companies/{slug}")
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    public PublicAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /** Inicios libres de un servicio en una fecha local de la empresa, con formato {@code AAAA-MM-DD} (US-06.2). */
    @GetMapping("/services/{serviceId}/availability")
    public AvailabilityResponse get(
            @PathVariable String slug, @PathVariable UUID serviceId, @RequestParam LocalDate date) {
        return availabilityService.publicAvailability(slug, serviceId, date);
    }
}
