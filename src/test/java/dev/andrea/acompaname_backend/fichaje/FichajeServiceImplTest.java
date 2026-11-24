package dev.andrea.acompaname_backend.fichaje;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTOResponse;
import dev.andrea.acompaname_backend.geolocation.Coordenadas;
import dev.andrea.acompaname_backend.geolocation.Geocodificador;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests de los fichajes de inicio y fin del servicio (sin base de datos: todo lo de fuera es falso)
@ExtendWith(MockitoExtension.class)
public class FichajeServiceImplTest {

    // El lugar del servicio, y puntos a unos 110 metros (cerca) y a unos 5,5 kilómetros (lejos)
    private static final Coordenadas LUGAR = new Coordenadas(43.3600, -5.8500);
    private static final FichajeDTORequest CERCA = new FichajeDTORequest(43.3610, -5.8500);
    private static final FichajeDTORequest LEJOS = new FichajeDTORequest(43.4100, -5.8500);

    @Mock
    private FichajeRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private Geocodificador geocodificador;

    private FichajeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FichajeServiceImpl(repository, solicitudRepository, geocodificador, 300);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private SolicitudEntity solicitud(EstadoSolicitud estado, LocalDate fecha, String direccion) {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setEmail("ana@test.com");
        UsuarioEntity usuarioCuidador = new UsuarioEntity();
        usuarioCuidador.setEmail("pepe@test.com");
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity();
        cuidador.setUsuario(usuarioCuidador);

        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setId(1L);
        solicitud.setEstado(estado);
        solicitud.setFechaCuidado(fecha);
        solicitud.setDireccion(direccion);
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        return solicitud;
    }

    private void hayUnaSolicitudAceptadaDeHoy() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1, Oviedo")));
    }

    private void elRepositorioGuardaLoQueRecibe() {
        when(repository.save(any(FichajeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FichajeEntity guardado() {
        ArgumentCaptor<FichajeEntity> captor = ArgumentCaptor.forClass(FichajeEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- Empezar ----------

    @Test
    void testEmpezarCercaDelLugarGuardaElInicioEnElLugar() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();
        when(geocodificador.buscar("Calle Uría 1, Oviedo")).thenReturn(Optional.of(LUGAR));

        FichajeDTOResponse resultado = service.empezar(1L, CERCA);

        assertThat(resultado.tipo(), is(equalTo(TipoFichaje.INICIO)));
        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.EN_EL_LUGAR)));
        FichajeEntity fichaje = guardado();
        assertThat(fichaje.getSolicitud().getId(), is(equalTo(1L)));
        assertThat(fichaje.getMomento().isAfter(LocalDateTime.now().minusMinutes(1)), is(true));
    }

    @Test
    void testEmpezarLejosDelLugarGuardaElInicioFueraDelLugar() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();
        when(geocodificador.buscar("Calle Uría 1, Oviedo")).thenReturn(Optional.of(LUGAR));

        FichajeDTOResponse resultado = service.empezar(1L, LEJOS);

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.FUERA_DEL_LUGAR)));
    }

    @Test
    void testElRadioSePuedeConfigurar() {
        // Con un radio de 50 metros, un punto a unos 110 metros ya está fuera
        service = new FichajeServiceImpl(repository, solicitudRepository, geocodificador, 50);
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();
        when(geocodificador.buscar("Calle Uría 1, Oviedo")).thenReturn(Optional.of(LUGAR));

        FichajeDTOResponse resultado = service.empezar(1L, CERCA);

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.FUERA_DEL_LUGAR)));
    }

    @Test
    void testSinUbicacionSeFichaIgualYNoSeConsultaLaDireccion() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();

        FichajeDTOResponse resultado = service.empezar(1L, new FichajeDTORequest(null, null));

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.SIN_UBICACION)));
        // La dirección privada solo sale al geocodificador si hay una ubicación que comparar
        verifyNoInteractions(geocodificador);
    }

    @Test
    void testSinCuerpoTambienSeFichaSinUbicacion() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();

        FichajeDTOResponse resultado = service.empezar(1L, null);

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.SIN_UBICACION)));
    }

    @Test
    void testSiLaDireccionNoSeEncuentraElFichajeEsNoVerificable() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();
        when(geocodificador.buscar("Calle Uría 1, Oviedo")).thenReturn(Optional.empty());

        FichajeDTOResponse resultado = service.empezar(1L, CERCA);

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.NO_VERIFICABLE)));
    }

    @Test
    void testSiLaSolicitudNoTieneDireccionElFichajeEsNoVerificable() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "  ")));
        elRepositorioGuardaLoQueRecibe();

        FichajeDTOResponse resultado = service.empezar(1L, CERCA);

        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.NO_VERIFICABLE)));
        verifyNoInteractions(geocodificador);
    }

    @Test
    void testEmpezarEnUnaSolicitudQueNoExisteDa404() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class, () -> service.empezar(9L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testSoloLaPersonaCuidadoraDeLaSolicitudPuedeEmpezar() {
        loguearComo("lucia@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.empezar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testLaFamiliaNoPuedeFichar() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.empezar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testSoloSePuedeFicharEnUnaSolicitudAceptada() {
        loguearComo("pepe@test.com");
        for (EstadoSolicitud estado : List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.RECHAZADA,
                EstadoSolicitud.COMPLETADA)) {
            when(solicitudRepository.findById(1L))
                    .thenReturn(Optional.of(solicitud(estado, LocalDate.now(), "Calle Uría 1")));

            assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.empezar(1L, CERCA));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void testNoSePuedeEmpezarAntesDelDiaDelServicio() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(
                Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now().plusDays(1), "Calle Uría 1")));

        assertThrows(SolicitudExceptionFechaInvalida.class, () -> service.empezar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testSePuedeEmpezarUnServicioDeDiasAnterioresQueSigueAceptado() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(
                Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now().minusDays(2), "Calle Uría 1")));
        elRepositorioGuardaLoQueRecibe();

        FichajeDTOResponse resultado = service.empezar(1L, null);

        assertThat(resultado.tipo(), is(equalTo(TipoFichaje.INICIO)));
    }

    @Test
    void testNoSePuedeEmpezarDosVeces() {
        hayUnaSolicitudAceptadaDeHoy();
        when(repository.existsBySolicitudIdAndTipo(1L, TipoFichaje.INICIO)).thenReturn(true);

        assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.empezar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    // ---------- Terminar ----------

    @Test
    void testTerminarGuardaElFinConSuUbicacion() {
        hayUnaSolicitudAceptadaDeHoy();
        elRepositorioGuardaLoQueRecibe();
        when(repository.existsBySolicitudIdAndTipo(1L, TipoFichaje.INICIO)).thenReturn(true);
        when(geocodificador.buscar("Calle Uría 1, Oviedo")).thenReturn(Optional.of(LUGAR));

        FichajeDTOResponse resultado = service.terminar(1L, CERCA);

        assertThat(resultado.tipo(), is(equalTo(TipoFichaje.FIN)));
        assertThat(resultado.ubicacion(), is(equalTo(ResultadoUbicacion.EN_EL_LUGAR)));
    }

    @Test
    void testNoSePuedeTerminarSinHaberEmpezado() {
        hayUnaSolicitudAceptadaDeHoy();

        assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.terminar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testNoSePuedeTerminarDosVeces() {
        hayUnaSolicitudAceptadaDeHoy();
        when(repository.existsBySolicitudIdAndTipo(1L, TipoFichaje.INICIO)).thenReturn(true);
        when(repository.existsBySolicitudIdAndTipo(1L, TipoFichaje.FIN)).thenReturn(true);

        assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.terminar(1L, CERCA));
        verify(repository, never()).save(any());
    }

    @Test
    void testSoloLaPersonaCuidadoraDeLaSolicitudPuedeTerminar() {
        loguearComo("lucia@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.terminar(1L, CERCA));
    }

    // ---------- Listar ----------

    private FichajeEntity fichaje(TipoFichaje tipo, ResultadoUbicacion ubicacion) {
        return FichajeEntity.builder()
                .id(tipo == TipoFichaje.INICIO ? 10L : 11L)
                .tipo(tipo)
                .momento(LocalDateTime.of(2026, 11, 24, 10, 0))
                .ubicacion(ubicacion)
                .build();
    }

    @Test
    void testLaFamiliaVeLosFichajesDeSuSolicitud() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));
        when(repository.findBySolicitudIdOrderByMomentoAsc(1L)).thenReturn(List.of(
                fichaje(TipoFichaje.INICIO, ResultadoUbicacion.EN_EL_LUGAR),
                fichaje(TipoFichaje.FIN, ResultadoUbicacion.SIN_UBICACION)));

        List<FichajeDTOResponse> resultado = service.listar(1L);

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).tipo(), is(equalTo(TipoFichaje.INICIO)));
        assertThat(resultado.get(0).ubicacion(), is(equalTo(ResultadoUbicacion.EN_EL_LUGAR)));
        assertThat(resultado.get(1).tipo(), is(equalTo(TipoFichaje.FIN)));
    }

    @Test
    void testLaPersonaCuidadoraVeSusFichajes() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));
        when(repository.findBySolicitudIdOrderByMomentoAsc(1L)).thenReturn(List.of());

        assertThat(service.listar(1L).isEmpty(), is(true));
    }

    @Test
    void testOtraPersonaNoPuedeVerLosFichajes() {
        loguearComo("lucia@test.com");
        when(solicitudRepository.findById(1L))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.ACEPTADA, LocalDate.now(), "Calle Uría 1")));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.listar(1L));
        verify(repository, never()).findBySolicitudIdOrderByMomentoAsc(any());
    }

    @Test
    void testListarEnUnaSolicitudQueNoExisteDa404() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class, () -> service.listar(9L));
    }

    @Test
    void testLaRespuestaNoLlevaCoordenadas() {
        // El DTO de respuesta solo tiene id, tipo, momento y ubicación (el resultado): ni latitud ni longitud
        FichajeDTOResponse dto = FichajeDTOResponse.de(fichaje(TipoFichaje.INICIO, ResultadoUbicacion.EN_EL_LUGAR));

        assertThat(dto.id(), is(equalTo(10L)));
        assertThat(dto.getClass().getRecordComponents().length, is(equalTo(4)));
        assertThat(FichajeDTOResponse.de(new FichajeEntity()).ubicacion(), is(nullValue()));
    }
}
