package dev.andrea.acompaname_backend.servicerequest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Overlapping dates")
public class SolicitudExceptionSolapamiento extends SolicitudException {
    public SolicitudExceptionSolapamiento(String message) {
        super(message);
    }

    public SolicitudExceptionSolapamiento(String message, Throwable cause) {
        super(message, cause);
    }

}