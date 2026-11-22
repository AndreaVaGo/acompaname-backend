package dev.andrea.acompaname_backend.verificacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que envía la persona cuidadora: solo una referencia, nunca un documento
public record EnviarVerificacionDTORequest(

        @NotBlank(message = "La referencia no puede estar vacía") @Size(max = 100, message = "La referencia no puede tener más de 100 caracteres") String referencia) {

}
