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

// Detrás de un proxy de confianza (en el test, la propia máquina: 127.0.0.1), la aplicación lee la IP
// real de la cabecera X-Forwarded-For. Así dos personas con IP distinta tienen contadores distintos.
// Se usa un servidor de verdad (RANDOM_PORT) porque la IP la resuelve Tomcat, no MockMvc.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.forward-headers-strategy=native",
        "rate-limit.max-requests=2",
        "rate-limit.window-seconds=30" })
public class SecurityLimiteIpRealProxyIntegrationTest extends ContenedorMySqlAplicacion {

    // Puerto aleatorio que Spring asigna al servidor del test
    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    // Intenta entrar con un usuario que no existe, diciendo que viene de esa IP
    private int intentarLoginDesde(String ip) throws Exception {
        String cuerpo = "{\"email\":\"nadie.proxy@test.com\",\"password\":\"Secreta123\"}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ip)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void testCadaIpRealTieneSuPropioContador() throws Exception {
        assertEquals(401, intentarLoginDesde("5.5.5.5"));
        assertEquals(401, intentarLoginDesde("5.5.5.5"));
        // La misma IP real ya llegó al máximo: se corta
        assertEquals(429, intentarLoginDesde("5.5.5.5"));

        // Otra persona, con otra IP real, no se ve afectada
        assertEquals(401, intentarLoginDesde("6.6.6.6"));
    }
}
