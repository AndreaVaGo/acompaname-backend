package dev.andrea.acompaname_backend.solicitud.exceptions;

public class SolicitudException extends RuntimeException {

    public SolicitudException(String message) {
        super(message);
    }

    public SolicitudException(String message, Throwable cause) {
        super(message, cause);
    }

}
