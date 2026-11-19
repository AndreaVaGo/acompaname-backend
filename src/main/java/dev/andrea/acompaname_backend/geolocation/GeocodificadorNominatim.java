package dev.andrea.acompaname_backend.geolocation;

import java.net.URI;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

// Busca lugares en Nominatim, el buscador de OpenStreetMap (gratis y sin clave de API).
// Reglas de uso de Nominatim que se respetan aquí:
// - máximo una consulta por segundo (se espera entre consultas)
// - identificarse con un User-Agent propio
// - no repetir consultas: se guarda en memoria lo que ya se ha buscado
@Service
@ConditionalOnProperty(name = "geocoding.enabled", havingValue = "true")
public class GeocodificadorNominatim implements Geocodificador {

    private static final Logger log = LoggerFactory.getLogger(GeocodificadorNominatim.class);

    // Para que alguien que escriba muchos lugares distintos no llene la memoria
    private static final int MAXIMO_EN_MEMORIA = 500;

    private final RestClient cliente;
    private final String urlBase;
    private final String agente;
    private final long pausaMs;

    // Lugares ya buscados (también los que no se encontraron). La clave es el lugar sin acentos ni mayúsculas
    private final Map<String, Optional<Coordenadas>> yaBuscados = new ConcurrentHashMap<>();
    private long ultimaConsulta = 0;

    @Autowired
    public GeocodificadorNominatim(
            @Value("${geocoding.url:https://nominatim.openstreetmap.org}") String urlBase,
            @Value("${geocoding.user-agent:Acompaname-Bootcamp/1.0 (proyecto educativo)}") String agente) {
        this(crearCliente(), urlBase, agente, 1100);
    }

    // Para los tests: se le pasa el cliente (falso) y sin pausa entre consultas
    GeocodificadorNominatim(RestClient cliente, String urlBase, String agente, long pausaMs) {
        this.cliente = cliente;
        this.urlBase = urlBase;
        this.agente = agente;
        this.pausaMs = pausaMs;
    }

    // Cliente con tiempos máximos: si Nominatim no responde, no se deja la web esperando
    private static RestClient crearCliente() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public Optional<Coordenadas> buscar(String lugar) {
        if (lugar == null || lugar.isBlank()) {
            return Optional.empty();
        }

        String clave = normalizar(lugar);
        Optional<Coordenadas> guardado = yaBuscados.get(clave);
        if (guardado != null) {
            return guardado;
        }
        return consultar(clave, lugar.trim());
    }

    // "Gijón " y "gijon" son el mismo lugar
    private String normalizar(String lugar) {
        String sinAcentos = Normalizer.normalize(lugar.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    // Una sola consulta a la vez, para poder respetar el límite de una por segundo
    private synchronized Optional<Coordenadas> consultar(String clave, String lugar) {
        // Otra persona pudo buscarlo mientras esperábamos el turno
        Optional<Coordenadas> guardado = yaBuscados.get(clave);
        if (guardado != null) {
            return guardado;
        }

        esperarTurno();
        try {
            // El lugar se codifica por completo: un "&" escrito por alguien no puede añadir parámetros
            URI uri = UriComponentsBuilder.fromUriString(urlBase)
                    .path("/search")
                    .queryParam("q", "{lugar}")
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("countrycodes", "es")
                    .encode()
                    .buildAndExpand(lugar)
                    .toUri();

            NominatimResultado[] resultados = cliente.get()
                    .uri(uri)
                    .header("User-Agent", agente)
                    .retrieve()
                    .body(NominatimResultado[].class);

            Optional<Coordenadas> encontrado = primerResultado(resultados);
            guardar(clave, encontrado);
            return encontrado;
        } catch (Exception e) {
            // Si falla (sin internet, Nominatim caído...) no se guarda, para volver a intentarlo más tarde
            log.warn("No se pudo buscar el lugar '{}' en Nominatim", lugar, e);
            return Optional.empty();
        } finally {
            ultimaConsulta = System.currentTimeMillis();
        }
    }

    private void esperarTurno() {
        long pasado = System.currentTimeMillis() - ultimaConsulta;
        if (pasado < pausaMs) {
            try {
                Thread.sleep(pausaMs - pasado);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Optional<Coordenadas> primerResultado(NominatimResultado[] resultados) {
        if (resultados == null || resultados.length == 0) {
            return Optional.empty();
        }
        NominatimResultado primero = resultados[0];
        return Optional.of(new Coordenadas(Double.parseDouble(primero.lat()), Double.parseDouble(primero.lon())));
    }

    private void guardar(String clave, Optional<Coordenadas> resultado) {
        if (yaBuscados.size() >= MAXIMO_EN_MEMORIA) {
            yaBuscados.clear();
        }
        yaBuscados.put(clave, resultado);
    }
}
