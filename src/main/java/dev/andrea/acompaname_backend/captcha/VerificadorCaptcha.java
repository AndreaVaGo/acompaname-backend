package dev.andrea.acompaname_backend.captcha;

// Comprueba que quien envía el formulario ha resuelto el CAPTCHA.
// Si no es válido, lanza CaptchaInvalidoException.
public interface VerificadorCaptcha {

    void verificar(String token, String ip);
}
