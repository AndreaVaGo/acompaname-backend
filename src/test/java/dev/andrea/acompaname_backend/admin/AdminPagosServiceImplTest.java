package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoCuidadorDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoMesDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests del resumen, el análisis y la lista de pagos del panel de administración
@ExtendWith(MockitoExtension.class)
public class AdminPagosServiceImplTest {
    @Mock
    private PagoRepository pagoRepository;

    private AdminPagosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminPagosServiceImpl(pagoRepository);
    }

    // Un pago de la familia Ana con un cuidador y un tipo de cuidado a elegir
    private PagoEntity pago(Long id, String importe, EstadoPago estado, LocalDate fecha, String tipoCuidado,
            Long cuidadorId, String nombreCuidador) {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setNombre("Ana");
        UsuarioEntity usuarioCuidador = new UsuarioEntity();
        usuarioCuidador.setNombre(nombreCuidador);
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity();
        cuidador.setId(cuidadorId);
        cuidador.setUsuario(usuarioCuidador);
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .id(id)
                .tipoCuidado(tipoCuidado)
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return PagoEntity.builder()
                .id(id)
                .importe(new BigDecimal(importe))
                .estado(estado)
                .fecha(fecha)
                .solicitud(solicitud)
                .build();
    }

    // Un pago de Pepe, a domicilio
    private PagoEntity pago(String importe, EstadoPago estado, LocalDate fecha) {
        return pago(1L, importe, estado, fecha, "domicilio", 1L, "Pepe");
    }

    @Test
    void testSinPagosTodoSaleACero() {
        when(pagoRepository.findAll()).thenReturn(List.of());

        AdminPagosResumenDTOResponse resultado = service.resumen();

        assertThat(resultado.totalCobrado(), is(comparesEqualTo(BigDecimal.ZERO)));
        assertThat(resultado.totalPendiente(), is(comparesEqualTo(BigDecimal.ZERO)));
        assertThat(resultado.numeroCobrados(), is(equalTo(0L)));
        assertThat(resultado.numeroPendientes(), is(equalTo(0L)));
        assertThat(resultado.numeroCancelados(), is(equalTo(0L)));
        assertThat(resultado.importeMedio(), is(comparesEqualTo(BigDecimal.ZERO)));
        assertThat(resultado.porcentajeCobrado(), is(nullValue()));
        assertThat(resultado.pendientesMasDeSieteDias(), is(equalTo(0L)));
        assertThat(resultado.porTipoCuidado(), hasSize(2));
        assertThat(resultado.porMes(), hasSize(6));
        assertThat(resultado.topCuidadores(), is(empty()));
    }

    @Test
    void testLosTotalesSeparanCobradosPendientesYCancelados() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago("36.00", EstadoPago.COMPLETADO, hoy),
                pago("18.00", EstadoPago.COMPLETADO, hoy),
                pago("36.00", EstadoPago.PENDIENTE, hoy),
                pago("100.00", EstadoPago.CANCELADO, hoy)));

        AdminPagosResumenDTOResponse resultado = service.resumen();

        assertThat(resultado.totalCobrado(), is(comparesEqualTo(new BigDecimal("54.00"))));
        assertThat(resultado.totalPendiente(), is(comparesEqualTo(new BigDecimal("36.00"))));
        assertThat(resultado.numeroCobrados(), is(equalTo(2L)));
        assertThat(resultado.numeroPendientes(), is(equalTo(1L)));
        assertThat(resultado.numeroCancelados(), is(equalTo(1L)));
    }

    @Test
    void testElImporteMedioYElPorcentajeIgnoranLosCancelados() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago("36.00", EstadoPago.COMPLETADO, hoy),
                pago("18.00", EstadoPago.COMPLETADO, hoy),
                pago("36.00", EstadoPago.PENDIENTE, hoy),
                pago("100.00", EstadoPago.CANCELADO, hoy)));

        AdminPagosResumenDTOResponse resultado = service.resumen();

        // 90 entre 3 pagos facturados = 30; 2 cobrados de 3 = 66,7 %
        assertThat(resultado.importeMedio(), is(comparesEqualTo(new BigDecimal("30.00"))));
        assertThat(resultado.porcentajeCobrado(), is(equalTo(66.7)));
    }

    @Test
    void testSoloAvisaDeLosPendientesDeMasDeSieteDias() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago("36.00", EstadoPago.PENDIENTE, hoy.minusDays(10)),
                // Justo hace siete días todavía no cuenta, igual que en el aviso del panel
                pago("10.00", EstadoPago.PENDIENTE, hoy.minusDays(7)),
                pago("20.00", EstadoPago.PENDIENTE, hoy.minusDays(3)),
                // Uno cobrado no es un aviso aunque sea antiguo
                pago("50.00", EstadoPago.COMPLETADO, hoy.minusDays(10))));

        AdminPagosResumenDTOResponse resultado = service.resumen();

        assertThat(resultado.pendientesMasDeSieteDias(), is(equalTo(1L)));
        assertThat(resultado.importePendienteMasDeSieteDias(), is(comparesEqualTo(new BigDecimal("36.00"))));
    }

    @Test
    void testAgrupaLoFacturadoPorTipoDeCuidado() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago(1L, "36.00", EstadoPago.COMPLETADO, hoy, "hospitalario", 1L, "Pepe"),
                pago(2L, "18.00", EstadoPago.PENDIENTE, hoy, "hospitalario", 1L, "Pepe"),
                pago(3L, "20.00", EstadoPago.COMPLETADO, hoy, "domicilio", 2L, "Marta"),
                // Un pago cancelado no se factura
                pago(4L, "50.00", EstadoPago.CANCELADO, hoy, "hospitalario", 1L, "Pepe")));

        AdminPagosResumenDTOResponse resultado = service.resumen();

        assertThat(resultado.porTipoCuidado().get(0).tipoCuidado(), is(equalTo("hospitalario")));
        assertThat(resultado.porTipoCuidado().get(0).pagos(), is(equalTo(2L)));
        assertThat(resultado.porTipoCuidado().get(0).importe(), is(comparesEqualTo(new BigDecimal("54.00"))));
        assertThat(resultado.porTipoCuidado().get(1).tipoCuidado(), is(equalTo("domicilio")));
        assertThat(resultado.porTipoCuidado().get(1).pagos(), is(equalTo(1L)));
        assertThat(resultado.porTipoCuidado().get(1).importe(), is(comparesEqualTo(new BigDecimal("20.00"))));
    }

    @Test
    void testElAnalisisPorMesTieneSeisMesesDelMasAntiguoAlActual() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago("36.00", EstadoPago.COMPLETADO, hoy),
                pago("18.00", EstadoPago.PENDIENTE, hoy),
                pago("20.00", EstadoPago.COMPLETADO, hoy.minusMonths(1)),
                // Este es de hace ocho meses: queda fuera del análisis
                pago("99.00", EstadoPago.COMPLETADO, hoy.minusMonths(8))));

        List<AdminPagoMesDTOResponse> meses = service.resumen().porMes();

        assertThat(meses, hasSize(6));
        assertThat(meses.get(0).mes(), is(equalTo(YearMonth.now().minusMonths(5).toString())));
        assertThat(meses.get(5).mes(), is(equalTo(YearMonth.now().toString())));
        assertThat(meses.get(5).cobrado(), is(comparesEqualTo(new BigDecimal("36.00"))));
        assertThat(meses.get(5).pendiente(), is(comparesEqualTo(new BigDecimal("18.00"))));
        assertThat(meses.get(4).cobrado(), is(comparesEqualTo(new BigDecimal("20.00"))));

        BigDecimal cobradoEnLosSeisMeses = BigDecimal.ZERO;
        for (AdminPagoMesDTOResponse mes : meses) {
            cobradoEnLosSeisMeses = cobradoEnLosSeisMeses.add(mes.cobrado());
        }
        assertThat(cobradoEnLosSeisMeses, is(comparesEqualTo(new BigDecimal("56.00"))));
    }

    @Test
    void testElTopSumaLosPagosCobradosDeCadaCuidadorYLosOrdena() {
        LocalDate hoy = LocalDate.now();
        when(pagoRepository.findAll()).thenReturn(List.of(
                pago(1L, "30.00", EstadoPago.COMPLETADO, hoy, "domicilio", 1L, "Ana"),
                pago(2L, "20.00", EstadoPago.COMPLETADO, hoy, "domicilio", 1L, "Ana"),
                pago(3L, "80.00", EstadoPago.COMPLETADO, hoy, "domicilio", 2L, "Beto"),
                pago(4L, "10.00", EstadoPago.COMPLETADO, hoy, "domicilio", 3L, "Carla"),
                // Lo pendiente de Carla no cuenta como cobrado
                pago(5L, "500.00", EstadoPago.PENDIENTE, hoy, "domicilio", 3L, "Carla")));

        List<AdminPagoCuidadorDTOResponse> top = service.resumen().topCuidadores();

        assertThat(top, hasSize(3));
        assertThat(top.get(0).nombre(), is(equalTo("Beto")));
        assertThat(top.get(0).pagos(), is(equalTo(1L)));
        assertThat(top.get(1).nombre(), is(equalTo("Ana")));
        assertThat(top.get(1).pagos(), is(equalTo(2L)));
        assertThat(top.get(1).importeCobrado(), is(comparesEqualTo(new BigDecimal("50.00"))));
        assertThat(top.get(2).nombre(), is(equalTo("Carla")));
        assertThat(top.get(2).importeCobrado(), is(comparesEqualTo(new BigDecimal("10.00"))));
    }

    @Test
    void testElTopSoloTieneCincoCuidadores() {
        LocalDate hoy = LocalDate.now();
        List<PagoEntity> pagos = new ArrayList<>();
        // Seis cuidadores que han cobrado 10, 20, 30, 40, 50 y 60
        for (long i = 1; i <= 6; i++) {
            pagos.add(pago(i, (i * 10) + ".00", EstadoPago.COMPLETADO, hoy, "domicilio", i, "Cuidador " + i));
        }
        when(pagoRepository.findAll()).thenReturn(pagos);

        List<AdminPagoCuidadorDTOResponse> top = service.resumen().topCuidadores();

        assertThat(top, hasSize(5));
        assertThat(top.get(0).nombre(), is(equalTo("Cuidador 6")));
        assertThat(top.get(4).nombre(), is(equalTo("Cuidador 2")));
    }

    @Test
    void testSinPagosLaListaEstaVacia() {
        when(pagoRepository.findAllByOrderByFechaDescIdDesc()).thenReturn(List.of());

        List<AdminPagoDTOResponse> resultado = service.pagos(null);

        assertThat(resultado, is(empty()));
    }

    @Test
    void testLaListaSinFiltroDevuelveTodosLosPagosConSusDatos() {
        LocalDate fecha = LocalDate.now().minusDays(3);
        when(pagoRepository.findAllByOrderByFechaDescIdDesc())
                .thenReturn(List.of(pago(7L, "36.00", EstadoPago.PENDIENTE, fecha, "hospitalario", 1L, "Pepe")));

        List<AdminPagoDTOResponse> resultado = service.pagos(null);

        assertThat(resultado, hasSize(1));
        AdminPagoDTOResponse fila = resultado.get(0);
        assertThat(fila.id(), is(equalTo(7L)));
        assertThat(fila.solicitudId(), is(equalTo(7L)));
        assertThat(fila.nombreFamilia(), is(equalTo("Ana")));
        assertThat(fila.nombreCuidador(), is(equalTo("Pepe")));
        assertThat(fila.tipoCuidado(), is(equalTo("hospitalario")));
        assertThat(fila.importe(), is(comparesEqualTo(new BigDecimal("36.00"))));
        assertThat(fila.estado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(fila.fecha(), is(equalTo(fecha)));
        assertThat(fila.diasDesdeCreacion(), is(equalTo(3L)));
    }

    @Test
    void testLaListaConEstadoPideSoloLosPagosDeEseEstado() {
        when(pagoRepository.findByEstadoOrderByFechaDescIdDesc(EstadoPago.COMPLETADO)).thenReturn(
                List.of(pago("36.00", EstadoPago.COMPLETADO, LocalDate.now())));

        List<AdminPagoDTOResponse> resultado = service.pagos(EstadoPago.COMPLETADO);

        assertThat(resultado, hasSize(1));
        assertThat(resultado.get(0).estado(), is(equalTo(EstadoPago.COMPLETADO)));
        verify(pagoRepository, never()).findAllByOrderByFechaDescIdDesc();
    }
}