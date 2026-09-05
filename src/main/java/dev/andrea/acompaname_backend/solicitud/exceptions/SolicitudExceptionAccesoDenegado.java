package dev.andrea.acompaname_backend.solicitud.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class SolicitudExceptionAccesoDenegado extends SolicitudException {
    public SolicitudExceptionAccesoDenegado(String message) {
        super(message);
    }

    public SolicitudExceptionAccesoDenegado(String message, Throwable cause) {
        super(message, cause);
    }

}
