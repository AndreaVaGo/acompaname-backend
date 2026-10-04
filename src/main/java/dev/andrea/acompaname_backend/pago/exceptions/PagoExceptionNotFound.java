package dev.andrea.acompaname_backend.pago.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Pago not found")
public class PagoExceptionNotFound extends PagoException {
    public PagoExceptionNotFound(String message) {
        super(message);
    }

    public PagoExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}