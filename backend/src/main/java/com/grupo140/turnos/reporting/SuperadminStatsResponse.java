package com.grupo140.turnos.reporting;

/**
 * Indicadores del dashboard del superadmin (US-04.6, reducida a dos indicadores en el MVP): empresas activas
 * e inactivas, y total de turnos. No incluye datos de clientes ni de ingresos.
 */
public record SuperadminStatsResponse(long activeCompanies, long inactiveCompanies, long totalAppointments) {}
