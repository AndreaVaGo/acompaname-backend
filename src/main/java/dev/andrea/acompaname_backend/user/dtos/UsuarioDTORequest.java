package dev.andrea.acompaname_backend.user.dtos;

import java.util.Set;

import dev.andrea.acompaname_backend.user.validation.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioDTORequest(

        @NotBlank(message = "El nombre no puede estar vacío") @NotNull(message = "El nombre no puede ser nulo") String nombre,

        @NotBlank(message = "El email no puede estar vacío") @Email(message = "El email no tiene un formato válido") String email,

        @NotBlank(message = "El telefono no puede estar vacío") String telefono,

        // @NotBlank exige que venga la contraseña; @PasswordSegura comprueba que cumple
        // las reglas (longitud, mayúscula, minúscula, número y carácter especial).
        // Sustituye al @Size que había antes: la longitud ya está incluida en @PasswordSegura.
        @NotBlank(message = "La password no puede estar vacía") @PasswordSegura String password,

        @NotNull(message = "Los roles no pueden ser nulos") Set<Long> rolesIds,

        // Token del CAPTCHA que da el widget del front. Solo se exige si captcha.enabled=true
        String captchaToken

) {

    // Sin CAPTCHA (tests, desarrollo): igual que antes
    public UsuarioDTORequest(String nombre, String email, String telefono, String password, Set<Long> rolesIds) {
        this(nombre, email, telefono, password, rolesIds, null);
    }

    // Igual que en el login: la contraseña no se escribe nunca en toString()
    @Override
    public String toString() {
        return "UsuarioDTORequest[nombre=" + nombre + ", email=" + email + ", telefono=" + telefono
                + ", password=***, rolesIds=" + rolesIds + "]";
    }
}