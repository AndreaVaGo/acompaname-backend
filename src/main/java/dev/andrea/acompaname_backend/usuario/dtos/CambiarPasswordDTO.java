package dev.andrea.acompaname_backend.usuario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordDTO(

        @NotBlank(message = "La contraseña actual no puede estar vacía") String passwordActual,

        @NotBlank(message = "La contraseña nueva no puede estar vacía") @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String passwordNueva

) {

}