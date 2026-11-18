package dev.andrea.acompaname_backend.schedule.exceptions;

public class BloqueOcupadoException extends RuntimeException {
    public BloqueOcupadoException(String message) {
        super(message);
    }

    public BloqueOcupadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
