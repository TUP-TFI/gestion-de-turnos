package com.grupo140.turnos.catalog;

import com.grupo140.turnos.company.PublicCompanyResponse;
import java.util.List;

/**
 * Respuesta de {@code GET /api/public/companies/{slug}} (US-06.1): la empresa y sus servicios activos. Si la
 * empresa está desactivada, {@code services} viene vacío.
 */
public record PublicPageResponse(PublicCompanyResponse company, List<PublicServiceResponse> services) {}
