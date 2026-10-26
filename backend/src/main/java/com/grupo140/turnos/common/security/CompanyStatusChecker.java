package com.grupo140.turnos.common.security;

import java.util.UUID;

/**
 * Consulta el estado de una empresa. {@code common} y {@code config} no pueden depender de {@code company}, así
 * que el filtro de seguridad habla con esta interfaz y {@code CompanyService} la implementa.
 */
public interface CompanyStatusChecker {

    /** Indica si la empresa existe y está activa. */
    boolean isActive(UUID companyId);
}
