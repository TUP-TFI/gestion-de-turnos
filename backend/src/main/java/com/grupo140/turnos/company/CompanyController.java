package com.grupo140.turnos.company;

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
 * Endpoints de empresas del panel del superadmin. El prefijo {@code /api/superadmin} ya exige el rol
 * {@code SUPERADMIN} (ver {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/superadmin/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /** Alta de empresa con su admin (US-04.1). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(request);
    }

    /** Listado de todas las empresas, ordenado por nombre (US-04.2). */
    @GetMapping
    public List<CompanyResponse> list() {
        return companyService.list();
    }

    /** Detalle de una empresa, para el formulario de edición (US-04.3). */
    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    /** Edición de los datos de una empresa, sin el slug ni el estado (US-04.3). */
    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CreateCompanyRequest.CompanyData request) {
        return companyService.update(id, request);
    }
}
