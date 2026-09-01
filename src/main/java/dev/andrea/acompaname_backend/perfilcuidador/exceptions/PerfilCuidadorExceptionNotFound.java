package dev.andrea.acompaname_backend.perfilcuidador.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Perfil cuidador not found")
public class PerfilCuidadorExceptionNotFound extends PerfilCuidadorException {
    public PerfilCuidadorExceptionNotFound(String message) {
        super(message);
    }

    public PerfilCuidadorExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }

}
