package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.dtos.SuscripcionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Vincular, sincronizar y desvincular, con un descargador falso (no se toca internet)
@ExtendWith(MockitoExtension.class)
public class SuscripcionCalendarioServiceImplTest {

    private static final String URL = "https://calendar.google.com/calendar/ical/x/private-abc/basic.ics";

    @Mock
    private SuscripcionCalendarioRepository repository;
    @Mock
    private PerfilCuidadorRepository perfilRepository;
    @Mock
    private DescargadorDeCalendario descargador;
    @Mock
    private GuardadoDeSincronizacion guardado;

    private final CifradorDeUrl cifrador = new CifradorDeUrl("frase-de-prueba", new String[] { "http://localhost" });

    private SuscripcionCalendarioServiceImpl service;
    private PerfilCuidadorEntity pepe;

    @BeforeEach
    void setup() {
        service = new SuscripcionCalendarioServiceImpl(repository, perfilRepository, descargador, guardado,
                cifrador);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("pepe@test.com", null));
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmail("pepe@test.com");
        pepe = new PerfilCuidadorEntity();
        pepe.setId(4L);
        pepe.setUsuario(usuario);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void darPerfil() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(pepe));
    }

    private String calendarioDeUnEvento() {
        String dia = LocalDate.now().plusDays(4).toString().replace("-", "");
        return "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Privado\r\nDTSTART:" + dia + "T090000\r\nDTEND:" + dia
                + "T110000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
    }

    private SuscripcionCalendarioEntity suscripcionDePepe() {
        return SuscripcionCalendarioEntity.builder().id(1L).cuidador(pepe).url(URL).build();
    }

    @Test
    void testEstadoSinVincularDiceQueNo() {
        darPerfil();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.empty());

        SuscripcionCalendarioDTOResponse estado = service.estado();

        assertThat(estado.vinculado(), is(false));
    }

    @Test
    void testEstadoVinculadoNoDevuelveLaDireccion() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        suscripcion.setUltimoError("Falló");
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));

        SuscripcionCalendarioDTOResponse estado = service.estado();

        assertThat(estado.vinculado(), is(true));
        assertThat(estado.ultimoError(), is(equalTo("Falló")));
        assertThat(estado.toString().contains("private-abc"), is(false));
    }

    @Test
    void testSinPerfilNoSePuedeVincular() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.vincular(URL));
        verify(guardado, never()).aplicar(any(), any());
    }

    @Test
    void testVincularLeeElCalendarioYGuardaLaSuscripcion() {
        darPerfil();
        when(descargador.normalizar(URL)).thenReturn(URL);
        when(descargador.descargar(URL)).thenReturn(calendarioDeUnEvento());
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.empty());

        SuscripcionCalendarioDTOResponse respuesta = service.vincular(URL);

        ArgumentCaptor<SuscripcionCalendarioEntity> captor = ArgumentCaptor.forClass(SuscripcionCalendarioEntity.class);
        ArgumentCaptor<LecturaDeCalendario> lectura = ArgumentCaptor.forClass(LecturaDeCalendario.class);
        verify(guardado).aplicar(captor.capture(), lectura.capture());
        // En la base de datos va cifrada, nunca en claro
        assertThat(captor.getValue().getUrl(), is(not(equalTo(URL))));
        assertThat(cifrador.descifrar(captor.getValue().getUrl()), is(equalTo(URL)));
        assertThat(captor.getValue().getCuidador(), is(equalTo(pepe)));
        assertThat(lectura.getValue().tramos().size(), is(1));
        assertThat(respuesta.vinculado(), is(true));
    }

    @Test
    void testVincularOtraVezCambiaLaDireccionDeLaMismaSuscripcion() {
        darPerfil();
        String nueva = "https://calendar.google.com/calendar/ical/y/private-zzz/basic.ics";
        SuscripcionCalendarioEntity existente = suscripcionDePepe();
        when(descargador.normalizar(nueva)).thenReturn(nueva);
        when(descargador.descargar(nueva)).thenReturn(calendarioDeUnEvento());
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(existente));

        service.vincular(nueva);

        verify(guardado).aplicar(any(SuscripcionCalendarioEntity.class), any(LecturaDeCalendario.class));
        assertThat(cifrador.estaCifrado(existente.getUrl()), is(true));
        assertThat(cifrador.descifrar(existente.getUrl()), is(equalTo(nueva)));
    }

    @Test
    void testSiLaDireccionNoValeNoSeGuardaNada() {
        darPerfil();
        when(descargador.normalizar("https://example.com/x.ics"))
                .thenThrow(new BloqueOcupadoExceptionFechaInvalida("Solo se pueden vincular calendarios..."));

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.vincular("https://example.com/x.ics"));

        verify(descargador, never()).descargar(anyString());
        verify(guardado, never()).aplicar(any(), any());
    }

    @Test
    void testSiLaDescargaFallaNoSeGuardaNada() {
        darPerfil();
        when(descargador.normalizar(URL)).thenReturn(URL);
        when(descargador.descargar(URL)).thenThrow(new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar"));

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.vincular(URL));

        verify(guardado, never()).aplicar(any(), any());
    }

    @Test
    void testSiLoDescargadoNoEsUnCalendarioNoSeGuardaNada() {
        darPerfil();
        when(descargador.normalizar(URL)).thenReturn(URL);
        when(descargador.descargar(URL)).thenReturn("<html>Inicia sesión</html>");

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.vincular(URL));

        verify(guardado, never()).aplicar(any(), any());
    }

    @Test
    void testSincronizarAhoraSinVincularDaError() {
        darPerfil();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.empty());

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.sincronizarAhora());
    }

    @Test
    void testSincronizarAhoraVuelveALeerYAplica() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));
        when(descargador.descargar(URL)).thenReturn(calendarioDeUnEvento());

        SuscripcionCalendarioDTOResponse respuesta = service.sincronizarAhora();

        verify(guardado).aplicar(any(SuscripcionCalendarioEntity.class), any(LecturaDeCalendario.class));
        assertThat(respuesta.vinculado(), is(true));
    }

    @Test
    void testSincronizarAhoraSiFallaApuntaElErrorYNoLanzaNada() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));
        when(descargador.descargar(URL)).thenThrow(new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar"));

        SuscripcionCalendarioDTOResponse respuesta = service.sincronizarAhora();

        verify(guardado).registrarError(suscripcion, "No se pudo descargar");
        verify(guardado, never()).aplicar(any(), any());
        assertThat(respuesta.vinculado(), is(true));
    }

    @Test
    void testDesvincularBorraLaSuscripcion() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));

        service.desvincular();

        verify(guardado).desvincular(suscripcion);
    }

    @Test
    void testDesvincularSinNadaVinculadoNoHaceNada() {
        darPerfil();
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.empty());

        service.desvincular();

        verify(guardado, never()).desvincular(any());
    }

    @Test
    void testSincronizarTodasSigueAunqueUnaFalle() {
        SuscripcionCalendarioEntity mala = SuscripcionCalendarioEntity.builder().id(1L).cuidador(pepe)
                .url("https://calendar.google.com/mala.ics").build();
        SuscripcionCalendarioEntity buena = SuscripcionCalendarioEntity.builder().id(2L).cuidador(pepe)
                .url(URL).build();
        when(repository.findAll()).thenReturn(List.of(mala, buena));
        when(descargador.descargar("https://calendar.google.com/mala.ics"))
                .thenThrow(new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar"));
        when(descargador.descargar(URL)).thenReturn(calendarioDeUnEvento());

        service.sincronizarTodas();

        verify(guardado).registrarError(mala, "No se pudo descargar");
        verify(guardado).aplicar(any(SuscripcionCalendarioEntity.class), any(LecturaDeCalendario.class));
    }

    @Test
    void testUnErrorInesperadoTampocoParaLaSincronizacion() {
        SuscripcionCalendarioEntity primera = suscripcionDePepe();
        when(repository.findAll()).thenReturn(List.of(primera));
        when(descargador.descargar(URL)).thenThrow(new IllegalStateException("fallo raro"));

        service.sincronizarTodas();

        verify(guardado).registrarError(primera, "fallo raro");
    }

    @Test
    void testUnaDireccionCifradaSeDescifraParaDescargar() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        suscripcion.setUrl(cifrador.cifrar(URL));
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));
        when(descargador.descargar(URL)).thenReturn(calendarioDeUnEvento());

        service.sincronizarAhora();

        verify(descargador).descargar(URL);
        verify(guardado).aplicar(any(SuscripcionCalendarioEntity.class), any(LecturaDeCalendario.class));
        assertThat(cifrador.descifrar(suscripcion.getUrl()), is(equalTo(URL)));
    }

    @Test
    void testUnaDireccionAntiguaEnClaroSeCifraTrasLeerseBien() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        suscripcion.setUrl(URL);
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));
        when(descargador.descargar(URL)).thenReturn(calendarioDeUnEvento());

        service.sincronizarAhora();

        assertThat(cifrador.estaCifrado(suscripcion.getUrl()), is(true));
        assertThat(cifrador.descifrar(suscripcion.getUrl()), is(equalTo(URL)));
    }

    @Test
    void testUnaDireccionAntiguaSeQuedaEnClaroSiLaLecturaFalla() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        suscripcion.setUrl(URL);
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));
        when(descargador.descargar(URL)).thenThrow(new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar"));

        service.sincronizarAhora();

        verify(guardado).registrarError(suscripcion, "No se pudo descargar");
    }

    @Test
    void testSiLaClaveHaCambiadoSeApuntaElErrorSinTocarInternet() {
        darPerfil();
        SuscripcionCalendarioEntity suscripcion = suscripcionDePepe();
        suscripcion.setUrl(new CifradorDeUrl("otra-frase", new String[] { "http://localhost" }).cifrar(URL));
        when(repository.findByCuidadorId(4L)).thenReturn(Optional.of(suscripcion));

        service.sincronizarAhora();

        verify(descargador, never()).descargar(anyString());
        verify(guardado).registrarError(any(SuscripcionCalendarioEntity.class), anyString());
    }
}
