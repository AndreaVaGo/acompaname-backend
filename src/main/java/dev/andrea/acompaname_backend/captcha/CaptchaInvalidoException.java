package dev.andrea.acompaname_backend.captcha;

// El CAPTCHA falta, no es válido o no se pudo comprobar
public class CaptchaInvalidoException extends RuntimeException {

    public CaptchaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
