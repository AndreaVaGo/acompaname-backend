package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// La dirección secreta del calendario no debe poder leerse en la base de datos (ACOM-261)
public class CifradorDeUrlTest {

    private static final String URL = "https://calendar.google.com/calendar/ical/x/private-abc/basic.ics";
    private static final String[] LOCAL = { "http://localhost:5173" };

    private final CifradorDeUrl cifrador = new CifradorDeUrl("una-frase-de-prueba", LOCAL);

    @Test
    void testCifrarYDescifrarDevuelveLaMismaDireccion() {
        assertThat(cifrador.descifrar(cifrador.cifrar(URL)), is(equalTo(URL)));
    }

    @Test
    void testLoCifradoNoContieneLaDireccion() {
        String cifrado = cifrador.cifrar(URL);

        assertThat(cifrado, startsWith(CifradorDeUrl.PREFIJO));
        assertThat(cifrado, not(containsString("private-abc")));
        assertThat(cifrado, not(containsString("calendar.google.com")));
        assertThat(cifrador.estaCifrado(cifrado), is(true));
    }

    @Test
    void testLaMismaDireccionDaValoresDistintosCadaVez() {
        assertThat(cifrador.cifrar(URL), is(not(equalTo(cifrador.cifrar(URL)))));
    }

    @Test
    void testUnaDireccionAntiguaEnClaroSeDevuelveTalCual() {
        assertThat(cifrador.estaCifrado(URL), is(false));
        assertThat(cifrador.descifrar(URL), is(equalTo(URL)));
    }

    @Test
    void testConOtraClaveNoSePuedeLeer() {
        String cifrado = cifrador.cifrar(URL);
        CifradorDeUrl otro = new CifradorDeUrl("otra-frase-distinta", LOCAL);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> otro.descifrar(cifrado));
        assertThat(error.getMessage(), not(containsString("private-abc")));
    }

    @Test
    void testUnValorManipuladoNoSePuedeLeer() {
        String cifrado = cifrador.cifrar(URL);
        char ultimo = cifrado.charAt(cifrado.length() - 5);
        String manipulado = cifrado.substring(0, cifrado.length() - 5) + (ultimo == 'A' ? 'B' : 'A')
                + cifrado.substring(cifrado.length() - 4);

        assertThrows(IllegalStateException.class, () -> cifrador.descifrar(manipulado));
    }

    @Test
    void testUnValorCifradoQueNoEsBase64NoSePuedeLeer() {
        assertThrows(IllegalStateException.class, () -> cifrador.descifrar(CifradorDeUrl.PREFIJO + "%%%"));
        assertThrows(IllegalStateException.class, () -> cifrador.descifrar(CifradorDeUrl.PREFIJO + "AAAA"));
    }

    @Test
    void testCabeEnLaColumnaConLaDireccionMasLargaPermitida() {
        String larga = "https://calendar.google.com/" + "a".repeat(DescargadorDeCalendario.MAXIMO_URL - 28);

        assertThat(cifrador.cifrar(larga).length() <= 1000, is(true));
    }

    @Test
    void testClaveDeDesarrolloConOrigenLocalArranca() {
        assertDoesNotThrow(() -> CifradorDeUrl.comprobar(CifradorDeUrl.CLAVE_DE_DESARROLLO, LOCAL));
    }

    @Test
    void testClaveDeDesarrolloConUnOrigenRealNoArranca() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> CifradorDeUrl
                .comprobar(CifradorDeUrl.CLAVE_DE_DESARROLLO, new String[] { "https://acompaname.es" }));

        assertThat(error.getMessage(), containsString("CALENDARIO_CLAVE"));
    }

    @Test
    void testUnaClavePropiaArrancaConCualquierOrigen() {
        assertDoesNotThrow(() -> CifradorDeUrl.comprobar("clave-propia-de-produccion",
                new String[] { "https://acompaname.es" }));
    }

    @Test
    void testUnaClaveVaciaNoArranca() {
        assertThrows(IllegalStateException.class, () -> CifradorDeUrl.comprobar(" ", LOCAL));
    }

    @Test
    void testLaClaveDeDesarrolloDelCodigoEsLaMismaQueLaDeApplicationProperties() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = new ClassPathResource("application.properties").getInputStream()) {
            propiedades.load(entrada);
        }

        assertThat(propiedades.getProperty("calendario.clave"),
                is(equalTo("${CALENDARIO_CLAVE:" + CifradorDeUrl.CLAVE_DE_DESARROLLO + "}")));
    }
}
