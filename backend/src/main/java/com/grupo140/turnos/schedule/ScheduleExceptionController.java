package com.grupo140.turnos.schedule;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de las excepciones de calendario del panel de la empresa (US-05.6). El prefijo
 * {@code /api/company} ya exige el rol {@code COMPANY_ADMIN} (ver {@code SecurityConfig}); la empresa sale de
 * la sesión, nunca del pedido.
 */
@RestController
@RequestMapping("/api/company/schedule-exceptions")
public class ScheduleExceptionController {

    private final ScheduleService scheduleService;

    public ScheduleExceptionController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** Excepciones de hoy en adelante, ordenadas por fecha. */
    @GetMapping
    public List<ScheduleExceptionResponse> list(SessionUser session) {
        return scheduleService.listExceptions(session.companyId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleExceptionResponse create(SessionUser session, @Valid @RequestBody ScheduleExceptionRequest request) {
        return scheduleService.createException(session.companyId(), request);
    }

    @PutMapping("/{id}")
    public ScheduleExceptionResponse update(
            SessionUser session, @PathVariable UUID id, @Valid @RequestBody ScheduleExceptionRequest request) {
        return scheduleService.updateException(session.companyId(), id, request);
    }

    /** Borrado físico: esa fecha vuelve a regirse por el horario semanal. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(SessionUser session, @PathVariable UUID id) {
        scheduleService.deleteException(session.companyId(), id);
    }
}
