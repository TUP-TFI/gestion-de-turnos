package com.grupo140.turnos.company;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Genera el slug de la URL pública ({@code /{slug}}) a partir del nombre de la empresa (US-04.1).
 *
 * <p>Si el slug ya existe o es una palabra reservada, le agrega un sufijo numérico: {@code -2},
 * {@code -3}, etc.
 */
@Component
public class SlugGenerator {

    // Largo de company.slug en la base.
    private static final int MAX_LENGTH = 60;

    // Rutas propias de la aplicación: un slug no puede coincidir con ninguna (ver diccionario-datos.md).
    private static final Set<String> RESERVED = Set.of(
            "api",
            "login",
            "logout",
            "admin",
            "superadmin",
            "company",
            "register",
            "activate",
            "reset-password",
            "assets",
            "static",
            "public",
            "docs",
            "health");

    // Slug de un nombre sin letras ni números (por ejemplo "!!!").
    private static final String FALLBACK = "empresa";

    private final CompanyRepository companyRepository;

    public SlugGenerator(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public String generate(String companyName) {
        String base = toSlug(companyName);
        String slug = base;
        for (int suffix = 2; isTaken(slug); suffix++) {
            String ending = "-" + suffix;
            slug = cut(base, MAX_LENGTH - ending.length()) + ending;
        }
        return slug;
    }

    private boolean isTaken(String slug) {
        return RESERVED.contains(slug) || companyRepository.existsBySlug(slug);
    }

    // Minúsculas, sin acentos y con guiones en lugar de espacios y símbolos: es el formato que exige el
    // CHECK chk_company_slug de la base.
    static String toSlug(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? FALLBACK : cut(slug, MAX_LENGTH);
    }

    // Recorta sin dejar un guion al final.
    private static String cut(String slug, int maxLength) {
        if (slug.length() <= maxLength) {
            return slug;
        }
        return slug.substring(0, maxLength).replaceAll("-$", "");
    }
}
