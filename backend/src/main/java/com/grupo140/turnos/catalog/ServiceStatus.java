package com.grupo140.turnos.catalog;

/** Estados posibles de un {@link Service}. Persistido como {@code varchar} + {@code CHECK} (ver esquema-bd.md). */
public enum ServiceStatus {
    ACTIVE,
    INACTIVE
}
