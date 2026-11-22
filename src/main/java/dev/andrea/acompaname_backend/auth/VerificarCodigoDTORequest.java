package dev.andrea.acompaname_backend.auth;

import jakarta.validation.constraints.NotBlank;

// Lo que envía el front en el segundo paso: el desafío que recibió al hacer login y el código del correo
public record VerificarCodigoDTORequest(
        @NotBlank(message = "El desafío es obligatorio") String desafio,
        @NotBlank(message = "El código es obligatorio") String codigo) {

}
