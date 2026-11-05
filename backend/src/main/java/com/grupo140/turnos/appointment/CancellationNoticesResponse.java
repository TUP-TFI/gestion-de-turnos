package com.grupo140.turnos.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lista de avisos de una empresa desactivada (US-04.4). El front arma con cada fila el link de WhatsApp, y muestra la
 * fecha en {@code timezone}, la zona de la empresa.
 */
public record CancellationNoticesResponse(String timezone, List<Notice> notices) {

    /**
     * Un turno cancelado por la baja. {@code phone} es nulo si el cliente no tiene teléfono: no hay a quién avisar.
     *
     * @param startDateTime instante en UTC.
     */
    public record Notice(
            UUID appointmentId, String clientName, String phone, String serviceName, Instant startDateTime) {}
}
