package com.grupo140.turnos.schedule;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del horario semanal del panel de la empresa. El prefijo {@code /api/company} ya exige el rol
 * {@code COMPANY_ADMIN} (ver {@code SecurityConfig}); la empresa sale de la sesión, nunca del pedido.
 */
@RestController
@RequestMapping("/api/company/business-hours")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** Franjas de la empresa, ordenadas por día y hora de inicio (US-05.3). */
    @GetMapping
    public List<BusinessHoursResponse> list(SessionUser session) {
        return scheduleService.list(session.companyId());
    }

    /** Reemplaza la semana entera; un día sin franjas queda cerrado (US-05.3). */
    @PutMapping
    public List<BusinessHoursResponse> replaceWeek(
            SessionUser session, @Valid @RequestBody BusinessHoursRequest request) {
        return scheduleService.replaceWeek(session.companyId(), request);
    }
}
