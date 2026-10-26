package com.grupo140.turnos.common.security;

import com.grupo140.turnos.common.error.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe un {@link ApiError} directo en la respuesta. Los errores de los filtros de seguridad ocurren antes del
 * MVC: el {@code GlobalExceptionHandler} no los atrapa y hay que escribirlos a mano.
 */
public final class ErrorResponseWriter {

    private ErrorResponseWriter() {}

    public static void write(
            HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), new ApiError(code, message));
    }
}
