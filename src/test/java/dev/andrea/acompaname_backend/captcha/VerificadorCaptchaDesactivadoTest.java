package dev.andrea.acompaname_backend.captcha;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

// Con el CAPTCHA desactivado nunca se rechaza nada
public class VerificadorCaptchaDesactivadoTest {

    private final VerificadorCaptchaDesactivado verificador = new VerificadorCaptchaDesactivado();

    @Test
    void testSinTokenNoLanzaNada() {
        assertDoesNotThrow(() -> verificador.verificar(null, "1.2.3.4"));
    }

    @Test
    void testConUnTokenCualquieraNoLanzaNada() {
        assertDoesNotThrow(() -> verificador.verificar("lo-que-sea", "1.2.3.4"));
    }
}
