package dev.andrea.acompaname_backend.auth;

import jakarta.validation.constraints.NotBlank;

// Lo que envía el front al iniciar sesión
public record LoginDTORequest(
        @NotBlank(message = "El email es obligatorio") String email,
        @NotBlank(message = "La contraseña es obligatoria") String password,
        // Token del CAPTCHA que da el widget del front. Solo se exige si captcha.enabled=true
        String captchaToken) {

    // Sin CAPTCHA (tests, desarrollo): igual que antes
    public LoginDTORequest(String email, String password) {
        this(email, password, null);
    }

    // Un record escribe todos sus campos en toString(). Lo cambiamos para que la
    // contraseña nunca acabe en un log ni en un mensaje de error por descuido.
    @Override
    public String toString() {
        return "LoginDTORequest[email=" + email + ", password=***]";
    }
}