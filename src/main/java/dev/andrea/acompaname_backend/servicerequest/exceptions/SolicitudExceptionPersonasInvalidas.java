package dev.andrea.acompaname_backend.servicerequest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// La lista de personas a cuidar no es válida (vacía o con más de 5)
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid people")
public class SolicitudExceptionPersonasInvalidas extends SolicitudException {
    public SolicitudExceptionPersonasInvalidas(String message) {
        super(message);
    }

    public SolicitudExceptionPersonasInvalidas(String message, Throwable cause) {
        super(message, cause);
    }

}
