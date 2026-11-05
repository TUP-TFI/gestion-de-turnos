package com.grupo140.turnos.company;

/**
 * Link de activación del admin de una empresa, para que el superadmin se lo mande por WhatsApp (US-04.1 y US-04.8).
 * El link lleva el token: solo lo recibe el superadmin, y no se escribe en el log.
 *
 * @param adminPhone teléfono del admin, tal como se cargó. El front arma con él el link de WhatsApp.
 */
public record ActivationLinkResponse(String activationLink, String adminPhone) {}
