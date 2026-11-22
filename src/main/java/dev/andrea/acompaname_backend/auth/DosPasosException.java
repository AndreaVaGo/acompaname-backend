package dev.andrea.acompaname_backend.auth;

// Error al activar o desactivar el inicio de sesión en dos pasos (por ejemplo, la contraseña no es correcta)
public class DosPasosException extends RuntimeException {

    public DosPasosException(String message) {
        super(message);
    }
}
