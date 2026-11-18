package dev.andrea.acompaname_backend.schedule.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Fecha u hora inválida")
public class BloqueOcupadoExceptionFechaInvalida extends BloqueOcupadoException {
    public BloqueOcupadoExceptionFechaInvalida(String message) {
        super(message);
    }
}
