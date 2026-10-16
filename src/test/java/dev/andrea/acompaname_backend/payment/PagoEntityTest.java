package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

public class PagoEntityTest {

    private SolicitudEntity crearSolicitud() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setId(1L);
        solicitud.setNombrePaciente("Manuel");
        return solicitud;
    }

    @Test
    void testPagoNuevoNoTieneDatos() {
        PagoEntity pago = new PagoEntity();

        assertThat(pago.getId(), is(nullValue()));
        assertThat(pago.getImporte(), is(nullValue()));
        assertThat(pago.getEstado(), is(nullValue()));
        assertThat(pago.getFecha(), is(nullValue()));
        assertThat(pago.getSolicitud(), is(nullValue()));
    }

    @Test
    void testConstructorConTodosLosDatos() {
        SolicitudEntity solicitud = crearSolicitud();

        PagoEntity pago = new PagoEntity(1L, new BigDecimal("36.00"), EstadoPago.PENDIENTE,
                LocalDate.of(2026, 10, 21), solicitud);

        assertThat(pago.getId(), is(equalTo(1L)));
        assertThat(pago.getImporte(), is(equalTo(new BigDecimal("36.00"))));
        assertThat(pago.getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pago.getFecha(), is(equalTo(LocalDate.of(2026, 10, 21))));
        assertThat(pago.getSolicitud(), is(equalTo(solicitud)));
    }

    @Test
    void testSettersYGetters() {
        SolicitudEntity solicitud = crearSolicitud();
        PagoEntity pago = new PagoEntity();
        pago.setId(2L);
        pago.setImporte(new BigDecimal("50.50"));
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setFecha(LocalDate.of(2026, 11, 5));
        pago.setSolicitud(solicitud);

        assertThat(pago.getId(), is(equalTo(2L)));
        assertThat(pago.getImporte(), is(equalTo(new BigDecimal("50.50"))));
        assertThat(pago.getEstado(), is(equalTo(EstadoPago.COMPLETADO)));
        assertThat(pago.getFecha(), is(equalTo(LocalDate.of(2026, 11, 5))));
        assertThat(pago.getSolicitud(), is(equalTo(solicitud)));
    }

    @Test
    void testBuilder() {
        SolicitudEntity solicitud = crearSolicitud();

        PagoEntity pago = PagoEntity.builder()
                .id(3L)
                .importe(new BigDecimal("20.00"))
                .estado(EstadoPago.PENDIENTE)
                .solicitud(solicitud)
                .build();

        assertThat(pago.getId(), is(equalTo(3L)));
        assertThat(pago.getImporte(), is(equalTo(new BigDecimal("20.00"))));
        assertThat(pago.getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pago.getSolicitud(), is(equalTo(solicitud)));
        assertThat(pago.getFecha(), is(nullValue()));
    }

    @Test
    void testPagoPasaDePendienteACompletado() {
        PagoEntity pago = new PagoEntity();
        pago.setEstado(EstadoPago.PENDIENTE);

        pago.setEstado(EstadoPago.COMPLETADO);

        assertThat(pago.getEstado(), is(equalTo(EstadoPago.COMPLETADO)));
    }

    @Test
    void testEstadosDelPago() {
        assertThat(EstadoPago.values().length, is(equalTo(3)));
        assertThat(EstadoPago.valueOf("CANCELADO"), is(equalTo(EstadoPago.CANCELADO)));
    }
}