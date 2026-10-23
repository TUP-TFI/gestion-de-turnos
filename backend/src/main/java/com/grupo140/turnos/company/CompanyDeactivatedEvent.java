package com.grupo140.turnos.company;

import java.util.UUID;

/**
 * Aviso de que una empresa pasó a {@link CompanyStatus#INACTIVE} (US-04.4). Lo publica {@link CompanyService} y
 * lo escucha {@code appointment} para cancelar los turnos pendientes de esa empresa.
 *
 * <p>Existe para que {@code company} no dependa de {@code appointment}: {@code appointment} ya depende de
 * {@code company}, y la llamada directa formaría un ciclo (ver la regla de dependencias del README).
 */
public record CompanyDeactivatedEvent(UUID companyId) {}
