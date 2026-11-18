package dev.andrea.acompaname_backend.schedule.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class BloqueOcupadoExceptionAccesoDenegado extends BloqueOcupadoException {
    public BloqueOcupadoExceptionAccesoDenegado(String message) {
        super(message);
    }
}
