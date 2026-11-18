package dev.andrea.acompaname_backend.passwordreset;

import dev.andrea.acompaname_backend.user.validation.PasswordSegura;
import jakarta.validation.constraints.NotBlank;

// Lo que envía el front al elegir la contraseña nueva: el token del enlace y la contraseña
public record RestablecerPasswordDTORequest(
        @NotBlank(message = "El token es obligatorio") String token,
        @NotBlank(message = "La contraseña nueva no puede estar vacía") @PasswordSegura String passwordNueva) {

    // Ni el token ni la contraseña se escriben nunca en toString()
    @Override
    public String toString() {
        return "RestablecerPasswordDTORequest[token=***, passwordNueva=***]";
    }
}
