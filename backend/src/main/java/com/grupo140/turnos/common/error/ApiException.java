package com.grupo140.turnos.common.error;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que el {@link GlobalExceptionHandler} traduce a {@link ApiError} con el status
 * indicado. Los services lanzan esta clase (o una subclase); nunca arman la respuesta HTTP.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
