package dev.andrea.acompaname_backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que envía el front para activar o desactivar el segundo paso
public record CambiarDosPasosDTORequest(
        @NotNull(message = "Indica si quieres activarlo o desactivarlo") Boolean activo,
        @NotBlank(message = "La contraseña es obligatoria") String password) {

    // Igual que en el login: la contraseña nunca debe acabar en un log por descuido
    @Override
    public String toString() {
        return "CambiarDosPasosDTORequest[activo=" + activo + ", password=***]";
    }
}
