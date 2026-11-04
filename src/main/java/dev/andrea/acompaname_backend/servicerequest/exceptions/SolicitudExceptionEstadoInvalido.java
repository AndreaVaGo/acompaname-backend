package dev.andrea.acompaname_backend.servicerequest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Invalid request state")
public class SolicitudExceptionEstadoInvalido extends SolicitudException {
    public SolicitudExceptionEstadoInvalido(String message) {
        super(message);
    }

    public SolicitudExceptionEstadoInvalido(String message, Throwable cause) {
        super(message, cause);
    }

}