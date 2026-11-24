package dev.andrea.acompaname_backend.fichaje;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;

public class FichajeDTORequestTest {

    @Test
    void testSinNingunaCoordenadaNoTieneUbicacionPeroEsValido() {
        FichajeDTORequest dto = new FichajeDTORequest(null, null);

        assertThat(dto.tieneUbicacion(), is(false));
        assertThat(dto.isUbicacionCompleta(), is(true));
    }

    @Test
    void testConLasDosCoordenadasTieneUbicacion() {
        FichajeDTORequest dto = new FichajeDTORequest(43.36, -5.85);

        assertThat(dto.tieneUbicacion(), is(true));
        assertThat(dto.isUbicacionCompleta(), is(true));
    }

    @Test
    void testConSoloUnaCoordenadaNoEsValido() {
        assertThat(new FichajeDTORequest(43.36, null).isUbicacionCompleta(), is(false));
        assertThat(new FichajeDTORequest(null, -5.85).isUbicacionCompleta(), is(false));
        assertThat(new FichajeDTORequest(43.36, null).tieneUbicacion(), is(false));
    }
}
