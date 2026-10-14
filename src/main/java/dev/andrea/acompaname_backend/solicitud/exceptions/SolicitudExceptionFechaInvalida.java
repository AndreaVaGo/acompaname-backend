package dev.andrea.acompaname_backend.solicitud.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid dates")
public class SolicitudExceptionFechaInvalida extends SolicitudException {
    public SolicitudExceptionFechaInvalida(String message) {
        super(message);
    }

    public SolicitudExceptionFechaInvalida(String message, Throwable cause) {
        super(message, cause);
    }

}