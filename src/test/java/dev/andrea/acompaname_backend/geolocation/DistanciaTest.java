package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class DistanciaTest {

    private static final Coordenadas OVIEDO = new Coordenadas(43.3614, -5.8593);
    private static final Coordenadas GIJON = new Coordenadas(43.5322, -5.6611);
    private static final Coordenadas MADRID = new Coordenadas(40.4168, -3.7038);
    private static final Coordenadas BARCELONA = new Coordenadas(41.3874, 2.1686);

    @Test
    void testElMismoPuntoEstaACeroKilometros() {
        assertThat(Distancia.enKilometros(OVIEDO, OVIEDO), is(0.0));
    }

    @Test
    void testOviedoYGijonEstanAUnos25Kilometros() {
        assertThat(Distancia.enKilometros(OVIEDO, GIJON), closeTo(25.0, 1.5));
    }

    @Test
    void testMadridYBarcelonaEstanAUnos505Kilometros() {
        assertThat(Distancia.enKilometros(MADRID, BARCELONA), closeTo(505.0, 5.0));
    }

    @Test
    void testLaDistanciaEsLaMismaIdaQueVuelta() {
        double ida = Distancia.enKilometros(OVIEDO, MADRID);
        double vuelta = Distancia.enKilometros(MADRID, OVIEDO);

        assertThat(ida, closeTo(vuelta, 0.0001));
    }

    @Test
    void testCruzandoElMeridianoDeGreenwichTambienSaleBien() {
        // Dos puntos a ambos lados de la longitud 0 y de la misma latitud: 0,2 grados de diferencia en el ecuador son unos 22 km
        double km = Distancia.enKilometros(new Coordenadas(0.0, -0.1), new Coordenadas(0.0, 0.1));

        assertThat(km, closeTo(22.2, 0.3));
    }
}
