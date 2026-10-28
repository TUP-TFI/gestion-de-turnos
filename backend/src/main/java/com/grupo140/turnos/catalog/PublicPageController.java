package com.grupo140.turnos.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Página pública de cada empresa. Va bajo {@code /api/public/**}: se ve sin login (ver {@code SecurityConfig}),
 * y la empresa sale del slug de la URL.
 */
@RestController
@RequestMapping("/api/public/companies/{slug}")
public class PublicPageController {

    private final CatalogService catalogService;

    public PublicPageController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Datos de la empresa y sus servicios activos (US-06.1). */
    @GetMapping
    public PublicPageResponse get(@PathVariable String slug) {
        return catalogService.publicPage(slug);
    }
}
