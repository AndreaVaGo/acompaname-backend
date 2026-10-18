package dev.andrea.acompaname_backend.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Conflict")
public class PagoExceptionConflicto extends PagoException {
    public PagoExceptionConflicto(String message) {
        super(message);
    }

    public PagoExceptionConflicto(String message, Throwable cause) {
        super(message, cause);
    }
}