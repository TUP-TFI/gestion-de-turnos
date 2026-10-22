package com.grupo140.turnos.user;

import java.util.UUID;

/**
 * Datos del admin de una empresa que necesitan otros features: el detalle de la empresa muestra el email y
 * el estado, y el reenvío del link de activación usa el id y el teléfono (US-04.8).
 */
public record CompanyAdmin(UUID id, String email, String phone, UserStatus status) {}
