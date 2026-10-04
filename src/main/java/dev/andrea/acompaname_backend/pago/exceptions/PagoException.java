package dev.andrea.acompaname_backend.pago.exceptions;

public class PagoException extends RuntimeException {
    public PagoException(String message) {
        super(message);
    }

    public PagoException(String message, Throwable cause) {
        super(message, cause);
    }
}