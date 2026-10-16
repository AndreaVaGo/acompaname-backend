package dev.andrea.acompaname_backend.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

public class ValoracionEntityTest {

    private SolicitudEntity crearSolicitud() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setId(1L);
        solicitud.setNombrePaciente("Manuel");
        return solicitud;
    }

    @Test
    void testValoracionNuevaNoTieneDatos() {
        ValoracionEntity valoracion = new ValoracionEntity();

        assertThat(valoracion.getId(), is(nullValue()));
        assertThat(valoracion.getComentario(), is(nullValue()));
        assertThat(valoracion.getPuntuacion(), is(nullValue()));
        assertThat(valoracion.getFecha(), is(nullValue()));
        assertThat(valoracion.getSolicitud(), is(nullValue()));
    }

    @Test
    void testConstructorConTodosLosDatos() {
        SolicitudEntity solicitud = crearSolicitud();

        ValoracionEntity valoracion = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 10, 14), solicitud);

        assertThat(valoracion.getId(), is(equalTo(1L)));
        assertThat(valoracion.getComentario(), is(equalTo("Muy buena atención")));
        assertThat(valoracion.getPuntuacion(), is(equalTo(5)));
        assertThat(valoracion.getFecha(), is(equalTo(LocalDate.of(2026, 10, 14))));
        assertThat(valoracion.getSolicitud(), is(equalTo(solicitud)));
    }

    @Test
    void testSettersYGetters() {
        SolicitudEntity solicitud = crearSolicitud();
        ValoracionEntity valoracion = new ValoracionEntity();
        valoracion.setId(2L);
        valoracion.setComentario("Todo correcto");
        valoracion.setPuntuacion(4);
        valoracion.setFecha(LocalDate.of(2026, 10, 15));
        valoracion.setSolicitud(solicitud);

        assertThat(valoracion.getId(), is(equalTo(2L)));
        assertThat(valoracion.getComentario(), is(equalTo("Todo correcto")));
        assertThat(valoracion.getPuntuacion(), is(equalTo(4)));
        assertThat(valoracion.getFecha(), is(equalTo(LocalDate.of(2026, 10, 15))));
        assertThat(valoracion.getSolicitud(), is(equalTo(solicitud)));
    }

    @Test
    void testBuilder() {
        SolicitudEntity solicitud = crearSolicitud();

        ValoracionEntity valoracion = ValoracionEntity.builder()
                .id(3L)
                .comentario("Puntual y amable")
                .puntuacion(5)
                .fecha(LocalDate.of(2026, 10, 16))
                .solicitud(solicitud)
                .build();

        assertThat(valoracion.getId(), is(equalTo(3L)));
        assertThat(valoracion.getComentario(), is(equalTo("Puntual y amable")));
        assertThat(valoracion.getPuntuacion(), is(equalTo(5)));
        assertThat(valoracion.getFecha(), is(equalTo(LocalDate.of(2026, 10, 16))));
        assertThat(valoracion.getSolicitud(), is(equalTo(solicitud)));
    }

    @Test
    void testValoracionSinComentario() {
        ValoracionEntity valoracion = ValoracionEntity.builder()
                .puntuacion(3)
                .build();

        assertThat(valoracion.getPuntuacion(), is(equalTo(3)));
        assertThat(valoracion.getComentario(), is(nullValue()));
    }

    @Test
    void testCambiarLaPuntuacion() {
        ValoracionEntity valoracion = new ValoracionEntity();
        valoracion.setPuntuacion(2);

        valoracion.setPuntuacion(4);

        assertThat(valoracion.getPuntuacion(), is(equalTo(4)));
    }
}