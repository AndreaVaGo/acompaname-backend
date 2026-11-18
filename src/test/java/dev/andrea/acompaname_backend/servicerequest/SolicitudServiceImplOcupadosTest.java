package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la búsqueda de cuidadores ocupados en unas fechas y horas (para filtrar en la búsqueda de la familia).
// Qué días se cruzan lo decide la consulta del repositorio, que se prueba con MySQL real
// en SolicitudRepositoryIntegrationTest. Aquí se prueba lo que hace el servicio: comprobar también las horas.
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplOcupadosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;

    private SolicitudServiceImpl service;

    private static final LocalDate DIA = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository);
    }

    // Solicitud aceptada de un cuidador, con las fechas y horas que le pasemos (las horas pueden ser null)
    private SolicitudEntity aceptada(Long cuidadorId, LocalDate inicio, LocalDate fin, LocalTime horaInicio,
            LocalTime horaFin) {
        PerfilCuidadorEntity cuidador = PerfilCuidadorEntity.builder().id(cuidadorId).build();
        return SolicitudEntity.builder()
                .estado(EstadoSolicitud.ACEPTADA)
                .fechaCuidado(inicio)
                .fechaFin(fin)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .cuidador(cuidador)
                .build();
    }

    // Servicio de un solo día con horas, del cuidador 1
    private SolicitudEntity deHoras(int desde, int hasta) {
        return aceptada(1L, DIA, null, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private void elRepositorioDevuelve(SolicitudEntity... solicitudes) {
        when(repository.findPorFechasQueSeCruzan(DIA, DIA, EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of(solicitudes));
    }

    @Test
    void testPideAlRepositorioLosServiciosAceptadosEnEseRangoDeDias() {
        when(repository.findPorFechasQueSeCruzan(DIA, LocalDate.of(2026, 9, 11), EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of(aceptada(1L, DIA, null, LocalTime.of(9, 0), LocalTime.of(12, 0))));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, LocalDate.of(2026, 9, 11), null, null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSinFechaFinSeBuscaUnSoloDia() {
        elRepositorioDevuelve(deHoras(9, 12));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, null, null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSiNadieEstaOcupadoDevuelveListaVacia() {
        when(repository.findPorFechasQueSeCruzan(DIA, LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of());

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, LocalDate.of(2026, 9, 12), null, null);

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testConFechaFinAnteriorALaDeInicioDaFechaInvalidaYNoConsultaLaBaseDeDatos() {
        assertThrows(SolicitudExceptionFechaInvalida.class,
                () -> service.getCuidadoresOcupados(DIA, LocalDate.of(2026, 9, 5), null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void testConHoraDeFinNoPosteriorALaDeInicioEnUnSoloDiaDaFechaInvalidaYNoConsultaLaBaseDeDatos() {
        assertThrows(SolicitudExceptionFechaInvalida.class,
                () -> service.getCuidadoresOcupados(DIA, null, LocalTime.of(15, 0), LocalTime.of(15, 0)));
        assertThrows(SolicitudExceptionFechaInvalida.class,
                () -> service.getCuidadoresOcupados(DIA, null, LocalTime.of(18, 0), LocalTime.of(15, 0)));
        verifyNoInteractions(repository);
    }

    // ---------- Horas ----------

    @Test
    void testUnServicioDeMananaNoOcupaAlCuidadorPorLaTarde() {
        elRepositorioDevuelve(deHoras(9, 12));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(15, 0), LocalTime.of(18, 0));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testUnServicioQueSeCruzaEnLasHorasOcupaAlCuidador() {
        elRepositorioDevuelve(deHoras(9, 12));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(11, 0), LocalTime.of(14, 0));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSiUnServicioAcabaJustoCuandoEmpiezaLaBusquedaElCuidadorEstaLibre() {
        elRepositorioDevuelve(deHoras(9, 12));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(12, 0), LocalTime.of(15, 0));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testSinHorasEnLaBusquedaCuentaElDiaCompleto() {
        elRepositorioDevuelve(deHoras(9, 12));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, null, null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testUnServicioAntiguoSinHorasOcupaElDiaCompleto() {
        elRepositorioDevuelve(aceptada(1L, DIA, null, null, null));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(15, 0), LocalTime.of(18, 0));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testUnCuidadorConDosServiciosQueSeCruzanApareceUnaSolaVez() {
        elRepositorioDevuelve(deHoras(9, 12), deHoras(10, 13));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSoloQuedaOcupadoElCuidadorCuyasHorasSeCruzan() {
        SolicitudEntity deManana = deHoras(9, 12);
        SolicitudEntity deTarde = aceptada(2L, DIA, null, LocalTime.of(15, 0), LocalTime.of(18, 0));
        elRepositorioDevuelve(deManana, deTarde);

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(16, 0), LocalTime.of(17, 0));

        assertThat(ocupados, is(equalTo(List.of(2L))));
    }

    @Test
    void testEnUnServicioDeVariosDiasLaHoraDeInicioCuentaElPrimerDiaYLaDeFinElUltimo() {
        // Del 10 a las 22:00 al 11 a las 08:00
        SolicitudEntity deNoche = aceptada(1L, DIA, LocalDate.of(2026, 9, 11), LocalTime.of(22, 0),
                LocalTime.of(8, 0));
        when(repository.findPorFechasQueSeCruzan(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 11),
                EstadoSolicitud.ACEPTADA)).thenReturn(List.of(deNoche));

        List<Long> aLasNueve = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 11), null, LocalTime.of(9, 0),
                LocalTime.of(12, 0));
        List<Long> aLasSiete = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 11), null, LocalTime.of(7, 0),
                LocalTime.of(10, 0));

        assertThat(aLasNueve, is(empty()));
        assertThat(aLasSiete, is(equalTo(List.of(1L))));
    }
}
