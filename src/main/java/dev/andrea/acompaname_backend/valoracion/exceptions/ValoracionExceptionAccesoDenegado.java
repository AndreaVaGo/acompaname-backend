package dev.andrea.acompaname_backend.valoracion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class ValoracionExceptionAccesoDenegado extends ValoracionException {
    public ValoracionExceptionAccesoDenegado(String message) {
        super(message);
    }

    public ValoracionExceptionAccesoDenegado(String message, Throwable cause) {
        super(message, cause);
    }
}
