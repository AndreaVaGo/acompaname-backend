package dev.andrea.acompaname_backend.caregiverprofile.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid photo")
public class PerfilCuidadorExceptionFotoInvalida extends PerfilCuidadorException {
    public PerfilCuidadorExceptionFotoInvalida(String message) {
        super(message);
    }

    public PerfilCuidadorExceptionFotoInvalida(String message, Throwable cause) {
        super(message, cause);
    }
}