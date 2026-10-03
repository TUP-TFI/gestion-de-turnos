package com.grupo140.turnos.company;

/** Rubros posibles de una {@link Company}. Persistido como {@code varchar} + {@code CHECK} (ver esquema-bd.md). */
public enum CompanyCategory {
    BARBERSHOP,
    HAIR_SALON,
    BEAUTY_CENTER,
    MEDICAL_OFFICE,
    DENTAL_OFFICE,
    VETERINARY,
    NUTRITION,
    PSYCHOLOGY,
    PHYSIOTHERAPY,
    TATTOO_STUDIO,
    SPA,
    OTHER
}
