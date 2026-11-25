package dev.andrea.acompaname_backend.geolocation;

import java.net.URI;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
// Y para que un abuso o una caída de Nominatim no deje la web esperando:
// - nadie espera su turno más de unos segundos: pasado ese tiempo se devuelve "no encontrado"
// - si Nominatim falla, se deja de consultar un rato en vez de reintentar en cada búsqueda
// - la memoria expulsa lo que menos se usa, no se vacía entera
@Service
@ConditionalOnProperty(name = "geocoding.enabled", havingValue = "true")
public class GeocodificadorNominatim implements Geocodificador {

    private static final Logger log = LoggerFactory.getLogger(GeocodificadorNominatim.class);

    // Para que alguien que escriba muchos lugares distintos no llene la memoria
    private static final int MAXIMO_EN_MEMORIA = 500;
    // Cuánto espera como máximo una búsqueda a que le toque el turno
    private static final long ESPERA_MAXIMA_MS = 8000;
    // Cuánto se deja de consultar a Nominatim después de un fallo
    private static final long ENFRIAMIENTO_TRAS_FALLO_MS = 30000;

    private final RestClient cliente;
    private final String urlBase;
    private final String agente;
    private final long pausaMs;
    private final long esperaMaximaMs;
    private final long enfriamientoMs;

    // Lugares ya buscados (también los que no se encontraron). La clave es el lugar sin acentos ni mayúsculas.
    // Al llegar al máximo se expulsa el que hace más tiempo que nadie pide (no se vacía entera)
    private final Map<String, Optional<Coordenadas>> yaBuscados;
    // Una sola consulta a la vez, para poder respetar el límite de una por segundo
    private final ReentrantLock turno = new ReentrantLock();
    private long ultimaConsulta = 0;
    // Hasta cuándo no se consulta, después de un fallo
    private volatile long sinConsultarHasta = 0;

    @Autowired
    public GeocodificadorNominatim(
            @Value("${geocoding.url:https://nominatim.openstreetmap.org}") String urlBase,
            @Value("${geocoding.user-agent:Acompaname-Bootcamp/1.0 (proyecto educativo)}") String agente) {
        this(crearCliente(), urlBase, agente, 1100, ESPERA_MAXIMA_MS, ENFRIAMIENTO_TRAS_FALLO_MS, MAXIMO_EN_MEMORIA);
    }

    // Para los tests: se le pasa el cliente (falso) y sin pausa entre consultas ni enfriamiento
    GeocodificadorNominatim(RestClient cliente, String urlBase, String agente, long pausaMs) {
        this(cliente, urlBase, agente, pausaMs, 5000, 0, MAXIMO_EN_MEMORIA);
    }

    GeocodificadorNominatim(RestClient cliente, String urlBase, String agente, long pausaMs, long esperaMaximaMs,
            long enfriamientoMs, int maximoEnMemoria) {
        this.cliente = cliente;
        this.urlBase = urlBase;
        this.agente = agente;
        this.pausaMs = pausaMs;
        this.esperaMaximaMs = esperaMaximaMs;
        this.enfriamientoMs = enfriamientoMs;
        this.yaBuscados = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Optional<Coordenadas>> masAntiguo) {
                return size() > maximoEnMemoria;
            }
        });
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

    private boolean enEnfriamiento() {
        return System.currentTimeMillis() < sinConsultarHasta;
    }

    private Optional<Coordenadas> consultar(String clave, String lugar) {
        // Si Nominatim acaba de fallar, no se insiste: se responde al momento
        if (enEnfriamiento()) {
            return Optional.empty();
        }

        if (!conseguirTurno()) {
            log.warn("Demasiadas búsquedas de lugares esperando turno: se responde sin consultar a Nominatim");
            return Optional.empty();
        }
        try {
            // Otra persona pudo buscarlo (o Nominatim pudo fallar) mientras esperábamos el turno
            Optional<Coordenadas> guardado = yaBuscados.get(clave);
            if (guardado != null) {
                return guardado;
            }
            if (enEnfriamiento()) {
                return Optional.empty();
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
                yaBuscados.put(clave, encontrado);
                return encontrado;
            } catch (Exception e) {
                // Si falla (sin internet, Nominatim caído...) no se guarda el lugar, para volver a intentarlo más
                // tarde, pero se deja de consultar un rato. El lugar no se escribe en el log: puede ser una dirección
                sinConsultarHasta = System.currentTimeMillis() + enfriamientoMs;
                log.warn("No se pudo consultar Nominatim: {}", e.toString());
                return Optional.empty();
            } finally {
                ultimaConsulta = System.currentTimeMillis();
            }
        } finally {
            turno.unlock();
        }
    }

    // Espera el turno como mucho esperaMaximaMs. Si no llega, no se consulta y no se bloquea más el hilo
    private boolean conseguirTurno() {
        try {
            return turno.tryLock(esperaMaximaMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
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
}
