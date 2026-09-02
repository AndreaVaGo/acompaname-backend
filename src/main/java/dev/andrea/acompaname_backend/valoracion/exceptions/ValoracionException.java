package dev.andrea.acompaname_backend.valoracion.exceptions;

public class ValoracionException extends RuntimeException {
    public ValoracionException(String message) {
        super(message);
    }

    public ValoracionException(String message, Throwable cause) {
        super(message, cause);
    }
}