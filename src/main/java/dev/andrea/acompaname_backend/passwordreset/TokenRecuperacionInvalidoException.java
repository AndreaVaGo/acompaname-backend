package dev.andrea.acompaname_backend.passwordreset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// El enlace de recuperación no existe, ya se usó o ha caducado.
// Siempre el mismo mensaje, para no dar pistas de cuál de los casos es.
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid reset token")
public class TokenRecuperacionInvalidoException extends RuntimeException {

    public TokenRecuperacionInvalidoException() {
        super("El enlace no es válido o ha caducado. Pide uno nuevo.");
    }
}
