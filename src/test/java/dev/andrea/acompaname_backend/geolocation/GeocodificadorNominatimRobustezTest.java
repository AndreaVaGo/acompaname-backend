package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Que un abuso o una caída de Nominatim no deje la web esperando
public class GeocodificadorNominatimRobustezTest {

    private static final String URL = "http://nominatim.test";
    private static final String JSON = "[{\"lat\":\"43.3602\",\"lon\":\"-5.8448\",\"display_name\":\"X\"}]";

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    @BeforeEach
    void setup() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private GeocodificadorNominatim crear(long esperaMaximaMs, long enfriamientoMs, int maximoEnMemoria) {
        return new GeocodificadorNominatim(builder.build(), URL, "Test-Agente", 0, esperaMaximaMs, enfriamientoMs,
                maximoEnMemoria);
    }

    @Test
    void testTrasUnFalloNoSeVuelveAConsultarDuranteElEnfriamiento() {
        GeocodificadorNominatim geocodificador = crear(5000, 60000, 500);
        // Solo se espera UNA petición: la segunda búsqueda no debe llegar a Nominatim
        servidor.expect(once(), requestTo(containsString("/search?q=Oviedo"))).andRespond(withServerError());

        Optional<Coordenadas> primera = geocodificador.buscar("Oviedo");
        Optional<Coordenadas> segunda = geocodificador.buscar("Gijon");

        assertThat(primera.isEmpty(), is(true));
        assertThat(segunda.isEmpty(), is(true));
        servidor.verify();
    }

    @Test
    void testLoQueYaEstabaEnMemoriaSigueRespondiendoDuranteElEnfriamiento() {
        GeocodificadorNominatim geocodificador = crear(5000, 60000, 500);
        servidor.expect(once(), requestTo(containsString("/search?q=Oviedo")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(containsString("/search?q=Gijon"))).andRespond(withServerError());

        geocodificador.buscar("Oviedo");
        geocodificador.buscar("Gijon");
        Optional<Coordenadas> deNuevoOviedo = geocodificador.buscar("Oviedo");

        assertThat(deNuevoOviedo.isPresent(), is(true));
        servidor.verify();
    }

    @Test
    void testQuienEsperaElTurnoMasDeLoPermitidoRecibeVacioSinConsultar() throws Exception {
        GeocodificadorNominatim geocodificador = crear(100, 0, 500);
        CountDownLatch consultando = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        // La primera consulta se queda "esperando a Nominatim" hasta que el test la libere
        servidor.expect(once(), requestTo(containsString("/search?q=Oviedo"))).andRespond(peticion -> {
            consultando.countDown();
            try {
                liberar.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return withSuccess(JSON, MediaType.APPLICATION_JSON).createResponse(peticion);
        });

        AtomicReference<Optional<Coordenadas>> deLaPrimera = new AtomicReference<>();
        Thread primera = new Thread(() -> deLaPrimera.set(geocodificador.buscar("Oviedo")));
        primera.start();
        consultando.await(5, TimeUnit.SECONDS);

        // Esta no consigue el turno en 100 ms: se va sin molestar a Nominatim
        Optional<Coordenadas> segunda = geocodificador.buscar("Gijon");

        liberar.countDown();
        primera.join(5000);
        assertThat(segunda.isEmpty(), is(true));
        assertThat(deLaPrimera.get().isPresent(), is(true));
        servidor.verify();
    }

    @Test
    void testAlLlegarAlMaximoSeExpulsaLoQueMenosSeUsaYNoTodo() {
        GeocodificadorNominatim geocodificador = crear(5000, 0, 2);
        servidor.expect(once(), requestTo(containsString("/search?q=Oviedo")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(containsString("/search?q=Gijon")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(containsString("/search?q=Aviles")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));
        // Gijon es lo que menos se ha usado, así que es lo único que hay que volver a pedir
        servidor.expect(once(), requestTo(containsString("/search?q=Gijon")))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

        geocodificador.buscar("Oviedo");
        geocodificador.buscar("Gijon");
        geocodificador.buscar("Oviedo");
        geocodificador.buscar("Aviles");
        Optional<Coordenadas> oviedo = geocodificador.buscar("Oviedo");
        geocodificador.buscar("Gijon");

        assertThat(oviedo.isPresent(), is(true));
        servidor.verify();
    }
}
