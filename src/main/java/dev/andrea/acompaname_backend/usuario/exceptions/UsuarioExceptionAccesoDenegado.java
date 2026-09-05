package dev.andrea.acompaname_backend.usuario.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class UsuarioExceptionAccesoDenegado extends UsuarioException {
    public UsuarioExceptionAccesoDenegado(String message) {
        super(message);
    }

    public UsuarioExceptionAccesoDenegado(String message, Throwable cause) {
        super(message, cause);
    }
}