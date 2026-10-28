package dev.andrea.acompaname_backend.globals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ListaDeTextoTest {

    @Test
    void testAtextoUneLosElementosConComas() {
        String texto = ListaDeTexto.aTexto(List.of("BEBES", "NINOS"));

        assertThat(texto, is(equalTo("BEBES,NINOS")));
    }

    @Test
    void testAtextoConUnSoloElementoNoPoneComas() {
        String texto = ListaDeTexto.aTexto(List.of("MAYORES"));

        assertThat(texto, is(equalTo("MAYORES")));
    }

    @Test
    void testAtextoConListaVaciaDevuelveNull() {
        assertThat(ListaDeTexto.aTexto(List.of()), is(nullValue()));
    }

    @Test
    void testAtextoConNullDevuelveNull() {
        assertThat(ListaDeTexto.aTexto(null), is(nullValue()));
    }

    @Test
    void testAlistaSeparaPorComas() {
        List<String> lista = ListaDeTexto.aLista("BEBES,NINOS,ADOLESCENTES");

        assertThat(lista, contains("BEBES", "NINOS", "ADOLESCENTES"));
    }

    @Test
    void testAlistaConUnSoloElemento() {
        List<String> lista = ListaDeTexto.aLista("MAYORES");

        assertThat(lista, contains("MAYORES"));
    }

    @Test
    void testAlistaConNullDevuelveListaVacia() {
        assertThat(ListaDeTexto.aLista(null), is(empty()));
    }

    @Test
    void testAlistaConTextoEnBlancoDevuelveListaVacia() {
        assertThat(ListaDeTexto.aLista("   "), is(empty()));
    }

    @Test
    void testIdaYVueltaDevuelveLaMismaLista() {
        List<String> original = List.of("ADULTOS", "DISCAPACIDAD_DEPENDENCIA");

        List<String> vuelta = ListaDeTexto.aLista(ListaDeTexto.aTexto(original));

        assertThat(vuelta, is(equalTo(original)));
    }
}