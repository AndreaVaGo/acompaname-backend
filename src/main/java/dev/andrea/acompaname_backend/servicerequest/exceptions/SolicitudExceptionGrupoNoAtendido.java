package dev.andrea.acompaname_backend.servicerequest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// La persona cuidadora no atiende alguno de los grupos de edad de la solicitud
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Age group not served")
public class SolicitudExceptionGrupoNoAtendido extends SolicitudException {
    public SolicitudExceptionGrupoNoAtendido(String message) {
        super(message);
    }

    public SolicitudExceptionGrupoNoAtendido(String message, Throwable cause) {
        super(message, cause);
    }

}
