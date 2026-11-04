package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

// Reglas de los cambios de estado de una solicitud:
// PENDIENTE -> ACEPTADA o RECHAZADA, ACEPTADA -> COMPLETADA, y de ahí no se sale.
public class EstadoSolicitudTest {

    @Test
    void testUnaSolicitudPendientePuedeAceptarse() {
        assertThat(EstadoSolicitud.PENDIENTE.puedePasarA(EstadoSolicitud.ACEPTADA), is(equalTo(true)));
    }

    @Test
    void testUnaSolicitudPendientePuedeRechazarse() {
        assertThat(EstadoSolicitud.PENDIENTE.puedePasarA(EstadoSolicitud.RECHAZADA), is(equalTo(true)));
    }

    @Test
    void testUnaSolicitudAceptadaPuedeCompletarse() {
        assertThat(EstadoSolicitud.ACEPTADA.puedePasarA(EstadoSolicitud.COMPLETADA), is(equalTo(true)));
    }

    @Test
    void testUnaSolicitudPendienteNoPuedeCompletarseDirectamente() {
        assertThat(EstadoSolicitud.PENDIENTE.puedePasarA(EstadoSolicitud.COMPLETADA), is(equalTo(false)));
    }

    @Test
    void testUnaSolicitudAceptadaNoPuedeRechazarseNiVolverAPendiente() {
        assertThat(EstadoSolicitud.ACEPTADA.puedePasarA(EstadoSolicitud.RECHAZADA), is(equalTo(false)));
        assertThat(EstadoSolicitud.ACEPTADA.puedePasarA(EstadoSolicitud.PENDIENTE), is(equalTo(false)));
    }

    @Test
    void testNingunEstadoPuedePasarASiMismo() {
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            assertThat(estado + " -> " + estado, estado.puedePasarA(estado), is(equalTo(false)));
        }
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = { "RECHAZADA", "COMPLETADA" })
    void testRechazadaYCompletadaSonEstadosFinalesYNoPuedenPasarANinguno(EstadoSolicitud final_) {
        for (EstadoSolicitud destino : EstadoSolicitud.values()) {
            assertThat(final_ + " -> " + destino, final_.puedePasarA(destino), is(equalTo(false)));
        }
    }

    @Test
    void testSoloUnaSolicitudPendienteAdmiteCambiosEnSusDatos() {
        assertThat(EstadoSolicitud.PENDIENTE.admiteCambios(), is(equalTo(true)));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, mode = EXCLUDE, names = { "PENDIENTE" })
    void testLosDemasEstadosNoAdmitenCambiosEnSusDatos(EstadoSolicitud estado) {
        assertThat(estado.admiteCambios(), is(equalTo(false)));
    }
}