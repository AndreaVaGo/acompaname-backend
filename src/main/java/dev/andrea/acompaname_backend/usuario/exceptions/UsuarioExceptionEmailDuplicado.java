package dev.andrea.acompaname_backend.usuario.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Email already registered")
public class UsuarioExceptionEmailDuplicado extends UsuarioException {
    public UsuarioExceptionEmailDuplicado(String message) {
        super(message);

    }

    public UsuarioExceptionEmailDuplicado(String message, Throwable cause) {
        super(message, cause);
    }
}
