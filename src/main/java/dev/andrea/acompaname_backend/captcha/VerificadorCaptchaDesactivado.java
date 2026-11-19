package dev.andrea.acompaname_backend.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Es el que se usa por defecto (captcha.enabled=false): no comprueba nada.
// Así el desarrollo local, los tests y las pruebas de carga funcionan sin CAPTCHA.
@Service
@ConditionalOnProperty(name = "captcha.enabled", havingValue = "false", matchIfMissing = true)
public class VerificadorCaptchaDesactivado implements VerificadorCaptcha {

    @Override
    public void verificar(String token, String ip) {
        // No hace nada a propósito
    }
}
