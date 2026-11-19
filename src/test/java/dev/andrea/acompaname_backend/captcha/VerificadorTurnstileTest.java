package dev.andrea.acompaname_backend.captcha;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Se prueba el verificador con un Cloudflare falso: nunca se sale a internet
public class VerificadorTurnstileTest {

    private static final String URL = "http://turnstile.test/siteverify";

    private MockRestServiceServer servidor;
    private VerificadorTurnstile verificador;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        verificador = new VerificadorTurnstile(builder.build(), URL, "clave-secreta");
    }

    @Test
    void testUnTokenBuenoPasa() {
        servidor.expect(once(), requestTo(URL))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> verificador.verificar("token-bueno", "1.2.3.4"));

        servidor.verify();
    }

    @Test
    void testUnTokenQueCloudflareRechazaLanzaLaExcepcion() {
        servidor.expect(once(), requestTo(URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
                        MediaType.APPLICATION_JSON));

        CaptchaInvalidoException error = assertThrows(CaptchaInvalidoException.class,
                () -> verificador.verificar("token-malo", "1.2.3.4"));

        assertThat(error.getMessage(), containsString("no es válido"));
        servidor.verify();
    }

    @Test
    void testSinTokenNiSePreguntaACloudflare() {
        servidor.expect(never(), requestTo(URL));

        assertThrows(CaptchaInvalidoException.class, () -> verificador.verificar(null, "1.2.3.4"));
        assertThrows(CaptchaInvalidoException.class, () -> verificador.verificar("", "1.2.3.4"));
        assertThrows(CaptchaInvalidoException.class, () -> verificador.verificar("   ", "1.2.3.4"));

        servidor.verify();
    }

    @Test
    void testSiCloudflareFallaSeRechazaYNoSeDejaPasar() {
        servidor.expect(once(), requestTo(URL)).andRespond(withServerError());

        CaptchaInvalidoException error = assertThrows(CaptchaInvalidoException.class,
                () -> verificador.verificar("token", "1.2.3.4"));

        assertThat(error.getMessage(), containsString("No se pudo comprobar"));
        servidor.verify();
    }

    @Test
    void testUnaRespuestaVaciaSeRechaza() {
        servidor.expect(once(), requestTo(URL)).andRespond(withSuccess());

        assertThrows(CaptchaInvalidoException.class, () -> verificador.verificar("token", "1.2.3.4"));
    }

    @Test
    void testMandaLaClaveSecretaElTokenYLaIpPorPost() {
        servidor.expect(once(), method(HttpMethod.POST))
                .andExpect(content().string(containsString("secret=clave-secreta")))
                .andExpect(content().string(containsString("response=mi-token")))
                .andExpect(content().string(containsString("remoteip=1.2.3.4")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        verificador.verificar("mi-token", "1.2.3.4");

        servidor.verify();
    }

    @Test
    void testSinIpTambienFunciona() {
        servidor.expect(once(), requestTo(URL))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> verificador.verificar("token", null));
    }

    @Test
    void testSiSeActivaSinClaveSecretaLaAplicacionNoArranca() {
        RestClient cliente = RestClient.builder().build();

        assertThrows(IllegalStateException.class, () -> new VerificadorTurnstile(cliente, URL, ""));
        assertThrows(IllegalStateException.class, () -> new VerificadorTurnstile(cliente, URL, null));
        assertThrows(IllegalStateException.class, () -> new VerificadorTurnstile(cliente, URL, "  "));
    }

    @Test
    void testElMensajeNoContieneLaClaveSecreta() {
        servidor.expect(once(), requestTo(URL)).andRespond(withServerError());

        CaptchaInvalidoException error = assertThrows(CaptchaInvalidoException.class,
                () -> verificador.verificar("token", "1.2.3.4"));

        assertThat(error.getMessage().contains("clave-secreta"), is(false));
    }
}
