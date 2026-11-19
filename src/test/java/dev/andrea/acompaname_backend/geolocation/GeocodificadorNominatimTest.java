package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class GeocodificadorNominatimTest {

    private static final String URL = "http://nominatim.test";
    private static final String OVIEDO_JSON = "[{\"lat\":\"43.3602\",\"lon\":\"-5.8448\",\"display_name\":\"Oviedo\"}]";

    private MockRestServiceServer servidor;
    private GeocodificadorNominatim geocodificador;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        // Sin pausa entre consultas para que los tests no esperen
        geocodificador = new GeocodificadorNominatim(builder.build(), URL, "Test-Agente", 0);
    }

    @Test
    void testEncuentraLasCoordenadasDeUnLugar() {
        servidor.expect(once(), requestTo(containsString(URL + "/search?q=Oviedo")))
                .andRespond(withSuccess(OVIEDO_JSON, MediaType.APPLICATION_JSON));

        Optional<Coordenadas> resultado = geocodificador.buscar("Oviedo");

        assertThat(resultado.isPresent(), is(true));
        assertThat(resultado.get().latitud(), closeTo(43.3602, 0.0001));
        assertThat(resultado.get().longitud(), closeTo(-5.8448, 0.0001));
        servidor.verify();
    }

    @Test
    void testSeIdentificaConSuPropioUserAgent() {
        servidor.expect(once(), header("User-Agent", "Test-Agente"))
                .andRespond(withSuccess(OVIEDO_JSON, MediaType.APPLICATION_JSON));

        geocodificador.buscar("Oviedo");

        servidor.verify();
    }

    @Test
    void testSoloPideUnResultadoYSoloEnEspana() {
        servidor.expect(once(), requestTo(allOf(containsString("limit=1"), containsString("countrycodes=es"))))
                .andRespond(withSuccess(OVIEDO_JSON, MediaType.APPLICATION_JSON));

        geocodificador.buscar("Oviedo");

        servidor.verify();
    }

    @Test
    void testUnLugarYaBuscadoNoSeVuelveAPedir() {
        // Solo se espera UNA petición: la segunda búsqueda sale de la memoria
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess(OVIEDO_JSON, MediaType.APPLICATION_JSON));

        geocodificador.buscar("Oviedo");
        Optional<Coordenadas> segunda = geocodificador.buscar("Oviedo");

        assertThat(segunda.isPresent(), is(true));
        servidor.verify();
    }

    @Test
    void testMayusculasAcentosYEspaciosSonElMismoLugar() {
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("[{\"lat\":\"43.5322\",\"lon\":\"-5.6611\"}]", MediaType.APPLICATION_JSON));

        geocodificador.buscar("Gijón");
        Optional<Coordenadas> otra = geocodificador.buscar("  gijon ");

        assertThat(otra.isPresent(), is(true));
        servidor.verify();
    }

    @Test
    void testUnLugarQueNoExisteDevuelveVacioYTampocoSeVuelveAPedir() {
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Optional<Coordenadas> primera = geocodificador.buscar("Lugarinventado");
        Optional<Coordenadas> segunda = geocodificador.buscar("Lugarinventado");

        assertThat(primera.isEmpty(), is(true));
        assertThat(segunda.isEmpty(), is(true));
        servidor.verify();
    }

    @Test
    void testSiElServicioFallaDevuelveVacioYSeVuelveAIntentarDespues() {
        // Primero falla y después responde bien: como el fallo no se guarda, la segunda búsqueda sí consulta
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withServerError());
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess(OVIEDO_JSON, MediaType.APPLICATION_JSON));

        Optional<Coordenadas> primera = geocodificador.buscar("Oviedo");
        Optional<Coordenadas> segunda = geocodificador.buscar("Oviedo");

        assertThat(primera.isEmpty(), is(true));
        assertThat(segunda.isPresent(), is(true));
        servidor.verify();
    }

    @Test
    void testUnLugarVacioNoConsultaNada() {
        // Sin ninguna petición esperada: si se consultara, el servidor falso daría error
        assertThat(geocodificador.buscar("").isEmpty(), is(true));
        assertThat(geocodificador.buscar("   ").isEmpty(), is(true));
        assertThat(geocodificador.buscar(null).isEmpty(), is(true));
        servidor.verify();
    }

    @Test
    void testLosCaracteresRarosDelLugarSeCodificanYNoAnadenParametros() {
        servidor.expect(once(), requestTo(not(containsString("limit=50"))))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        geocodificador.buscar("Oviedo&limit=50");

        servidor.verify();
    }

    @Test
    void testUnResultadoConCoordenadasRarasDevuelveVacio() {
        servidor.expect(once(), requestTo(containsString("/search")))
                .andRespond(withSuccess("[{\"lat\":\"abc\",\"lon\":\"def\"}]", MediaType.APPLICATION_JSON));

        assertThat(geocodificador.buscar("Oviedo").isEmpty(), is(true));
    }
}
