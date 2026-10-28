package dev.andrea.acompaname_backend.user.dtos;

import dev.andrea.acompaname_backend.user.validation.PasswordSegura;
import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordDTO(

        // La contraseña actual NO lleva @PasswordSegura a propósito: las cuentas creadas
        // antes de esta regla pueden tener una contraseña débil, y deben poder escribirla
        // para demostrar que son ellas y cambiarla por una segura.
        @NotBlank(message = "La contraseña actual no puede estar vacía") String passwordActual,

        // La contraseña nueva sí debe cumplir la regla, igual que en el registro.
        @NotBlank(message = "La contraseña nueva no puede estar vacía") @PasswordSegura String passwordNueva

) {

}