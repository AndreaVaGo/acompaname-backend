package dev.andrea.acompaname_backend.pago.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class PagoExceptionAccesoDenegado extends PagoException {
    public PagoExceptionAccesoDenegado(String message) {
        super(message);
    }

    public PagoExceptionAccesoDenegado(String message, Throwable cause) {
        super(message, cause);
    }
}