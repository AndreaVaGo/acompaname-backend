package dev.andrea.acompaname_backend.solicitud.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Solicitud not found")
public class SolicitudExceptionNotFound extends SolicitudException {
    public SolicitudExceptionNotFound(String message) {
        super(message);
    }

    public SolicitudExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }

}
