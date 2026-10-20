package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ValoracionRepository valoracionRepository;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminServiceImpl(usuarioRepository, perfilCuidadorRepository, solicitudRepository,
                pagoRepository, valoracionRepository);
    }

    // Solicitud creada y aceptada en las horas indicadas del 10 de noviembre
    private SolicitudEntity solicitudAceptada(int horaCreacion, int horaAceptacion) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setFechaCreacion(LocalDateTime.of(2026, 11, 10, horaCreacion, 0));
        solicitud.setFechaAceptacion(LocalDateTime.of(2026, 11, 10, horaAceptacion, 0));
        return solicitud;
    }

    @Test
    void testSinDatosTodoSaleACeroOVacio() {
        // Un mock devuelve 0.0 si no se le dice nada; la base de datos real devuelve null sin valoraciones
        when(valoracionRepository.mediaGeneral()).thenReturn(null);

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.totalFamilias(), is(equalTo(0L)));
        assertThat(resultado.totalCuidadores(), is(equalTo(0L)));
        assertThat(resultado.solicitudesPendientes(), is(equalTo(0L)));
        assertThat(resultado.ingresosCobrados(), is(equalTo(BigDecimal.ZERO)));
        assertThat(resultado.ingresosPendientes(), is(equalTo(BigDecimal.ZERO)));
        assertThat(resultado.valoracionMedia(), is(nullValue()));
        assertThat(resultado.horasMediasHastaAceptar(), is(nullValue()));
    }

    @Test
    void testDevuelveLosConteosDeUsuariosYCuidadores() {
        when(usuarioRepository.countByRolesName("FAMILIA")).thenReturn(10L);
        when(usuarioRepository.countByRolesName("CUIDADOR")).thenReturn(5L);
        when(perfilCuidadorRepository.countByDisponibleAhoraTrue()).thenReturn(3L);

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.totalFamilias(), is(equalTo(10L)));
        assertThat(resultado.totalCuidadores(), is(equalTo(5L)));
        assertThat(resultado.cuidadoresDisponibles(), is(equalTo(3L)));
    }

    @Test
    void testDevuelveLasSolicitudesPorEstadoYPorTipo() {
        when(solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE)).thenReturn(4L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.ACEPTADA)).thenReturn(6L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA)).thenReturn(1L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.COMPLETADA)).thenReturn(2L);
        when(solicitudRepository.countByTipoCuidado("hospitalario")).thenReturn(7L);
        when(solicitudRepository.countByTipoCuidado("domicilio")).thenReturn(6L);

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.solicitudesPendientes(), is(equalTo(4L)));
        assertThat(resultado.solicitudesAceptadas(), is(equalTo(6L)));
        assertThat(resultado.solicitudesRechazadas(), is(equalTo(1L)));
        assertThat(resultado.solicitudesCompletadas(), is(equalTo(2L)));
        assertThat(resultado.solicitudesHospital(), is(equalTo(7L)));
        assertThat(resultado.solicitudesDomicilio(), is(equalTo(6L)));
    }

    @Test
    void testDevuelveLosIngresosCobradosYPendientes() {
        when(pagoRepository.sumaPorEstado(EstadoPago.COMPLETADO)).thenReturn(new BigDecimal("120.00"));
        when(pagoRepository.sumaPorEstado(EstadoPago.PENDIENTE)).thenReturn(new BigDecimal("36.00"));

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.ingresosCobrados(), is(equalTo(new BigDecimal("120.00"))));
        assertThat(resultado.ingresosPendientes(), is(equalTo(new BigDecimal("36.00"))));
    }

    @Test
    void testLaValoracionMediaSeRedondeaAUnDecimal() {
        when(valoracionRepository.mediaGeneral()).thenReturn(4.26);

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.valoracionMedia(), is(equalTo(4.3)));
    }

    @Test
    void testLasHorasMediasHastaAceptarSeCalculanConLasSolicitudesAceptadas() {
        // Una tardó 2 horas y la otra 1 hora: la media es 1,5
        when(solicitudRepository.findByFechaAceptacionIsNotNull())
                .thenReturn(List.of(solicitudAceptada(10, 12), solicitudAceptada(10, 11)));

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.horasMediasHastaAceptar(), is(equalTo(1.5)));
    }

    @Test
    void testSeIgnoranLasSolicitudesSinFechaDeCreacion() {
        // La solicitud antigua no tiene fecha de creación: no cuenta para la media
        SolicitudEntity antigua = new SolicitudEntity();
        antigua.setFechaAceptacion(LocalDateTime.of(2026, 11, 10, 12, 0));
        when(solicitudRepository.findByFechaAceptacionIsNotNull())
                .thenReturn(List.of(solicitudAceptada(10, 12), antigua));

        EstadisticasDTOResponse resultado = service.estadisticas();

        assertThat(resultado.horasMediasHastaAceptar(), is(equalTo(2.0)));
    }
}