package dev.andrea.acompaname_backend.usuario.dtos;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDTORequest(

        @NotBlank(message = "El nombre no puede estar vacío") @NotNull(message = "El nombre no puede ser nulo") String nombre,

        @NotBlank(message = "El email no puede estar vacío") @Email(message = "El email no tiene un formato válido") String email,

        @NotBlank(message = "El telefono no puede estar vacío") String telefono,

        @NotBlank(message = "La password no puede estar vacía") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,

        @NotNull(message = "Los roles no pueden ser nulos") Set<Long> rolesIds

) {

}