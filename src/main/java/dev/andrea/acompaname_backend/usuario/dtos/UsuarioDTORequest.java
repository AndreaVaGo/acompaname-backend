package dev.andrea.acompaname_backend.usuario.dtos;

import dev.andrea.acompaname_backend.usuario.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioDTORequest(

        @NotBlank(message = "El nombre no puede estar vacío") @NotNull(message = "El nombre no puede ser nulo") String nombre,

        @NotBlank(message = "El email no puede estar vacío") String email,

        @NotBlank(message = "El telefono no puede estar vacío") String telefono,

        @NotBlank(message = "La password no puede estar vacía") String password,

        @NotNull(message = "El rol no puede ser nulo") Rol rol

) {
}
