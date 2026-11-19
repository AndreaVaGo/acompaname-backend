package dev.andrea.acompaname_backend.captcha;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

// Comprueba el CAPTCHA con Cloudflare Turnstile (captcha.enabled=true).
// El front manda el token que le da el widget; aquí se le pregunta a Cloudflare si es bueno.
// Si Cloudflare no responde, se rechaza (es mejor cerrar la puerta que dejarla abierta).
@Service
@ConditionalOnProperty(name = "captcha.enabled", havingValue = "true")
public class VerificadorTurnstile implements VerificadorCaptcha {

    private static final Logger log = LoggerFactory.getLogger(VerificadorTurnstile.class);

    private static final String MENSAJE = "No se pudo comprobar el CAPTCHA. Inténtalo de nuevo.";

    private final RestClient cliente;
    private final String url;
    private final String clave;

    @Autowired
    public VerificadorTurnstile(
            @Value("${captcha.url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String url,
            @Value("${captcha.secret:}") String clave) {
        this(crearCliente(), url, clave);
    }

    // Para los tests: se le pasa el cliente (falso)
    VerificadorTurnstile(RestClient cliente, String url, String clave) {
        // Si se activa el CAPTCHA sin clave secreta, la aplicación no arranca
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("captcha.enabled=true necesita la variable CAPTCHA_SECRET");
        }
        this.cliente = cliente;
        this.url = url;
        this.clave = clave;
    }

    // Tiempos máximos: si Cloudflare no responde, no se deja la web esperando
    private static RestClient crearCliente() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public void verificar(String token, String ip) {
        // Sin token ni se pregunta a Cloudflare
        if (token == null || token.isBlank()) {
            throw new CaptchaInvalidoException("Completa el CAPTCHA para continuar.");
        }

        MultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("secret", clave);
        datos.add("response", token);
        if (ip != null) {
            datos.add("remoteip", ip);
        }

        RespuestaTurnstile respuesta;
        try {
            respuesta = cliente.post().uri(url).body(datos).retrieve().body(RespuestaTurnstile.class);
        } catch (Exception e) {
            // Sin internet, Cloudflare caído...: no se deja pasar
            log.warn("No se pudo comprobar el CAPTCHA con Turnstile", e);
            throw new CaptchaInvalidoException(MENSAJE);
        }

        if (respuesta == null || !respuesta.success()) {
            throw new CaptchaInvalidoException("El CAPTCHA no es válido. Inténtalo de nuevo.");
        }
    }

    // Lo único que se lee de la respuesta de Cloudflare: {"success": true}
    record RespuestaTurnstile(boolean success) {
    }
}
