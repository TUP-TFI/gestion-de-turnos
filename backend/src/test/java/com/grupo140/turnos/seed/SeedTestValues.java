package com.grupo140.turnos.seed;

/**
 * Valores del seed de demo en el perfil {@code dev}, para que los tests de integración puedan iniciar
 * sesión y activar cuentas. Tienen que coincidir con {@code app.seed.*} de {@code application-dev.yml}.
 */
public final class SeedTestValues {

    public static final String DEV_PASSWORD = "Turnos.dev.2026";

    public static final String DEV_ACTIVATION_TOKEN = "dev-activation-token";

    private SeedTestValues() {}
}
