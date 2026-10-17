package com.grupo140.turnos.catalog;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del catálogo de servicios del panel de la empresa. El prefijo {@code /api/company} ya exige
 * el rol {@code COMPANY_ADMIN} (ver {@code SecurityConfig}); la empresa sale de la sesión, nunca del pedido.
 */
@RestController
@RequestMapping("/api/company/services")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Listado de los servicios de la empresa, activos e inactivos, ordenado por nombre (US-05.1). */
    @GetMapping
    public List<ServiceResponse> list(SessionUser session) {
        return catalogService.list(session.companyId());
    }

    /** Detalle de un servicio, para el formulario de edición (US-05.1). */
    @GetMapping("/{id}")
    public ServiceResponse get(SessionUser session, @PathVariable UUID id) {
        return catalogService.get(session.companyId(), id);
    }

    /** Alta de un servicio (US-05.1). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(SessionUser session, @Valid @RequestBody ServiceRequest request) {
        return catalogService.create(session.companyId(), request);
    }

    /** Edición de un servicio (US-05.1). */
    @PutMapping("/{id}")
    public ServiceResponse update(
            SessionUser session, @PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return catalogService.update(session.companyId(), id, request);
    }

    /** Desactivación de un servicio, que deja de ofrecerse a los clientes (US-05.2). */
    @PostMapping("/{id}/deactivate")
    public ServiceResponse deactivate(SessionUser session, @PathVariable UUID id) {
        return catalogService.deactivate(session.companyId(), id);
    }

    /** Reactivación de un servicio desactivado (US-05.2). */
    @PostMapping("/{id}/activate")
    public ServiceResponse activate(SessionUser session, @PathVariable UUID id) {
        return catalogService.activate(session.companyId(), id);
    }
}
