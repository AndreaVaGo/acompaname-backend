package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class CodigoLoginEntityTest {

    @Test
    void testUnCodigoNuevoEmpiezaConCeroIntentos() {
        CodigoLoginEntity codigo = CodigoLoginEntity.builder().expiraEn(LocalDateTime.now().plusMinutes(10)).build();

        assertThat(codigo.getIntentos(), is(equalTo(0)));
    }

    @Test
    void testSumarIntentoSubeElContador() {
        CodigoLoginEntity codigo = CodigoLoginEntity.builder().expiraEn(LocalDateTime.now().plusMinutes(10)).build();

        codigo.sumarIntento();
        codigo.sumarIntento();

        assertThat(codigo.getIntentos(), is(equalTo(2)));
    }

    @Test
    void testNoEstaCaducadoAntesDeSuHora() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 22, 10, 0);
        CodigoLoginEntity codigo = CodigoLoginEntity.builder().expiraEn(ahora.plusMinutes(1)).build();

        assertThat(codigo.estaCaducado(ahora), is(false));
    }

    @Test
    void testEstaCaducadoDespuesDeSuHora() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 22, 10, 0);
        CodigoLoginEntity codigo = CodigoLoginEntity.builder().expiraEn(ahora.minusSeconds(1)).build();

        assertThat(codigo.estaCaducado(ahora), is(true));
    }
}
