package dev.andrea.acompaname_backend.verificacion.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Decisión del administrador: aprobar (true) o rechazar (false). Al rechazar se puede explicar el motivo
public record ResolverVerificacionDTORequest(

        @NotNull(message = "Tienes que indicar si se aprueba o se rechaza") Boolean aprobar,

        @Size(max = 200, message = "El motivo no puede tener más de 200 caracteres") String motivo) {

}
