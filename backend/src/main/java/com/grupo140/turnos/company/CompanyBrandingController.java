package com.grupo140.turnos.company;

import com.grupo140.turnos.common.security.SessionUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marca de la propia empresa, desde el panel del admin (US-05.7). El prefijo {@code /api/company} ya exige el
 * rol {@code COMPANY_ADMIN} (ver {@code SecurityConfig}), y la empresa sale siempre de la sesión.
 */
@RestController
@RequestMapping("/api/company/branding")
public class CompanyBrandingController {

    private final CompanyService companyService;

    public CompanyBrandingController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /** Logo y color primario actuales de la empresa. */
    @GetMapping
    public BrandingResponse get(SessionUser session) {
        return companyService.getBranding(session.companyId());
    }

    /** Reemplaza el logo y el color: lo que llegue en {@code null} se borra. */
    @PutMapping
    public BrandingResponse update(SessionUser session, @Valid @RequestBody BrandingRequest request) {
        return companyService.updateBranding(session.companyId(), request);
    }
}
