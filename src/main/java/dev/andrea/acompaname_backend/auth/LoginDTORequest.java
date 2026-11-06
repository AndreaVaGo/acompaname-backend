package dev.andrea.acompaname_backend.auth;

import jakarta.validation.constraints.NotBlank;

// Lo que envía el front al iniciar sesión
public record LoginDTORequest(
        @NotBlank(message = "El email es obligatorio") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}