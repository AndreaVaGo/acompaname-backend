package dev.andrea.acompaname_backend.schedule.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Bloque ocupado not found")
public class BloqueOcupadoExceptionNotFound extends BloqueOcupadoException {
    public BloqueOcupadoExceptionNotFound(String message) {
        super(message);
    }
}
