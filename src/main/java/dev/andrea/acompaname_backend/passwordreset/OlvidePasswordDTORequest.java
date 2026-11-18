package dev.andrea.acompaname_backend.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Lo que envía el front al pedir el enlace para recuperar la contraseña
public record OlvidePasswordDTORequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no tiene un formato válido") String email) {
}
