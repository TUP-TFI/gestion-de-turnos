package com.grupo140.turnos.common.error;

/**
 * Cuerpo de toda respuesta de error de la API (US-03.10).
 *
 * @param code identificador estable y legible por máquina ({@code INVALID_CREDENTIALS}). El frontend
 *     decide qué hacer según este valor.
 * @param message texto en español para mostrar al usuario.
 */
public record ApiError(String code, String message) {}
