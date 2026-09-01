package dev.andrea.acompaname_backend.usuario.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Usuario not found")
public class UsuarioExceptionNotFound extends UsuarioException {
    public UsuarioExceptionNotFound(String message) {
        super(message);
    }

    public UsuarioExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}