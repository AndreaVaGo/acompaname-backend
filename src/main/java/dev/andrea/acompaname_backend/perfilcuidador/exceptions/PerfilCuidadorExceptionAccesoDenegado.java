package dev.andrea.acompaname_backend.perfilcuidador.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class PerfilCuidadorExceptionAccesoDenegado extends PerfilCuidadorException {
    public PerfilCuidadorExceptionAccesoDenegado(String message) {
        super(message);
    }

    public PerfilCuidadorExceptionAccesoDenegado(String message, Throwable cause) {
        super(message, cause);
    }
}