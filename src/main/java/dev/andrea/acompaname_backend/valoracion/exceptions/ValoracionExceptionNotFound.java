package dev.andrea.acompaname_backend.valoracion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Valoracion not found")
public class ValoracionExceptionNotFound extends ValoracionException {
    public ValoracionExceptionNotFound(String message) {
        super(message);
    }

    public ValoracionExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}