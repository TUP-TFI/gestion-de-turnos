package com.grupo140.turnos.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce toda excepción que sale de un controller al formato único {@link ApiError} (US-03.10).
 *
 * <p>Los errores de seguridad (401 y 403 del filtro) ocurren antes del MVC y no pasan por acá: los
 * escribe {@code SecurityConfig} con el mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String APPOINTMENT_OVERLAP_CONSTRAINT = "excl_appointment_overlap";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Los datos enviados no son válidos");
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "El cuerpo de la petición es inválido"));
    }

    // Un valor de la dirección que no se puede convertir al tipo esperado, por ejemplo un id que no es UUID
    // (/api/superadmin/companies/abc). Es un error del pedido, no del servidor.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "La dirección tiene un valor que no es válido"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", "El recurso no existe"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("FORBIDDEN", "No tenés permisos para esta operación"));
    }

    // Segunda barrera de la reserva (US-07.3): si dos turnos superpuestos llegan a la base, responde lo mismo que la
    // validación del service. Se mira el mensaje de Postgres porque Hibernate no extrae el nombre de una constraint
    // de exclusión (SQLState 23P01): getConstraintName() viene nulo. El resto sigue como un error no controlado.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String databaseMessage = ex.getMostSpecificCause().getMessage();
        if (databaseMessage != null && databaseMessage.contains(APPOINTMENT_OVERLAP_CONSTRAINT)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("SLOT_UNAVAILABLE", "El horario ya no está disponible"));
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Errores HTTP de Spring (405, 415, etc.) conservan su status en lugar de volverse 500.
        if (ex instanceof ErrorResponse errorResponse
                && errorResponse.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(new ApiError("BAD_REQUEST", "La petición no es válida"));
        }
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Ocurrió un error inesperado. Intentá de nuevo más tarde"));
    }
}
