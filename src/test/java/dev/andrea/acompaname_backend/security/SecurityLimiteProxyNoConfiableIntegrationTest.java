package dev.andrea.acompaname_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Si la petición NO llega desde un proxy de confianza, la cabecera X-Forwarded-For se ignora.
// Aquí el único proxy de confianza es 10.1.1.1, así que la máquina del test (127.0.0.1) no lo es:
// quien intente saltarse el límite inventándose la cabecera sigue contando como la misma IP.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=10\\.1\\.1\\.1",
        "rate-limit.max-requests=2",
        "rate-limit.window-seconds=30" })
public class SecurityLimiteProxyNoConfiableIntegrationTest extends ContenedorMySqlAplicacion {

    // Puerto aleatorio que Spring asigna al servidor del test
    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private int intentarLoginDesde(String ipFalsa) throws Exception {
        String cuerpo = "{\"email\":\"nadie.falso@test.com\",\"password\":\"Secreta123\"}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ipFalsa)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void testUnaCabeceraFalsaDeUnProxyNoConfiableSeIgnora() throws Exception {
        assertEquals(401, intentarLoginDesde("7.7.7.7"));
        assertEquals(401, intentarLoginDesde("8.8.8.8"));

        // Aunque diga ser otra IP, no viene de un proxy de confianza: sigue siendo la misma
        assertEquals(429, intentarLoginDesde("9.9.9.9"));
    }
}
