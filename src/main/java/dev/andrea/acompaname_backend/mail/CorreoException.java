package dev.andrea.acompaname_backend.mail;

// Se lanza cuando no se ha podido enviar un correo (por ejemplo, si el servidor SMTP no responde)
public class CorreoException extends RuntimeException {

    public CorreoException(String message, Throwable cause) {
        super(message, cause);
    }
}
