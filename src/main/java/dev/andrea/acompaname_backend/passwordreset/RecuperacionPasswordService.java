package dev.andrea.acompaname_backend.passwordreset;

public interface RecuperacionPasswordService {

    // Manda por correo un enlace para elegir una contraseña nueva.
    // No dice nada si el correo no existe, para que nadie pueda averiguar quién tiene cuenta
    void solicitarRestablecimiento(String email);

    // Cambia la contraseña si el token es válido; después el token ya no sirve
    void restablecer(String token, String passwordNueva);
}
