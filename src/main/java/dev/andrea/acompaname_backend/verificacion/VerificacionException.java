package dev.andrea.acompaname_backend.verificacion;

// La verificación no se puede hacer (por ejemplo, ya está verificada o ya no está pendiente)
public class VerificacionException extends RuntimeException {

    public VerificacionException(String message) {
        super(message);
    }
}
