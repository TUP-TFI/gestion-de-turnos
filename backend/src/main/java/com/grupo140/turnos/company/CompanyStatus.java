package com.grupo140.turnos.company;

/** Estados posibles de una {@link Company}. Persistido como {@code varchar} + {@code CHECK} (ver esquema-bd.md). */
public enum CompanyStatus {
    ACTIVE,
    INACTIVE
}
