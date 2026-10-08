package dev.andrea.acompaname_backend.valoracion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Conflict")
public class ValoracionExceptionConflicto extends ValoracionException {
    public ValoracionExceptionConflicto(String message) {
        super(message);
    }

    public ValoracionExceptionConflicto(String message, Throwable cause) {
        super(message, cause);
    }
}