package dev.andrea.acompaname_backend.user.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid password")
public class UsuarioExceptionPasswordInvalida extends UsuarioException {
    public UsuarioExceptionPasswordInvalida(String message) {
        super(message);
    }

    public UsuarioExceptionPasswordInvalida(String message, Throwable cause) {
        super(message, cause);
    }
}