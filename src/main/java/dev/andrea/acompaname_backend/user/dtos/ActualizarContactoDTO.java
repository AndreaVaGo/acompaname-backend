package dev.andrea.acompaname_backend.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarContactoDTO(

        @NotBlank(message = "El nombre no puede estar vacío") String nombre,

        @NotBlank(message = "El email no puede estar vacío") @Email(message = "El email no tiene un formato válido") String email,

        @NotBlank(message = "El teléfono no puede estar vacío") String telefono

) {

}