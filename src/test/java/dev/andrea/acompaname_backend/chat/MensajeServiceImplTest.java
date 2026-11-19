package dev.andrea.acompaname_backend.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.chat.dtos.MensajesNoLeidosDTOResponse;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionConflicto;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Reglas del chat: solo hablan la familia y la persona cuidadora de la solicitud
@ExtendWith(MockitoExtension.class)
public class MensajeServiceImplTest {

    @Mock
    private MensajeRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private AvisosDeChat avisos;

    private MensajeServiceImpl service;
    private SecurityContext contextoOriginal;
    private UsuarioEntity ana;
    private UsuarioEntity pepe;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new MensajeServiceImpl(repository, solicitudRepository, avisos);
        ana = usuario(1L, "Ana", "ana@test.com");
        pepe = usuario(2L, "Pepe", "pepe@test.com");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private UsuarioEntity usuario(Long id, String nombre, String email) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    // Solicitud de Ana (familia) a Pepe (cuidador) en el estado que le pasemos
    private SolicitudEntity solicitud(EstadoSolicitud estado) {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setUsuario(pepe);
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .id(5L)
                .estado(estado)
                .familia(ana)
                .cuidador(perfil)
                .build();
        when(solicitudRepository.findById(5L)).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private MensajeEntity mensaje(Long id, UsuarioEntity emisor, String texto, SolicitudEntity solicitud) {
        return MensajeEntity.builder()
                .id(id)
                .solicitud(solicitud)
                .emisor(emisor)
                .texto(texto)
                .fecha(LocalDateTime.of(2026, 11, 18, 10, 0))
                .build();
    }

    @Test
    void testLaFamiliaPuedeEscribir() {
        solicitud(EstadoSolicitud.PENDIENTE);
        loguearComo("ana@test.com");
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> {
            MensajeEntity guardado = inv.getArgument(0);
            guardado.setId(10L);
            return guardado;
        });

        MensajeDTOResponse resultado = service.enviar(5L, new MensajeDTORequest("Hola Pepe"));

        assertThat(resultado.id(), is(equalTo(10L)));
        assertThat(resultado.emisorNombre(), is(equalTo("Ana")));
        assertThat(resultado.texto(), is(equalTo("Hola Pepe")));
        assertThat(resultado.mio(), is(equalTo(true)));
    }

    @Test
    void testLaPersonaCuidadoraPuedeResponderEnUnaSolicitudAceptada() {
        solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("pepe@test.com");
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MensajeDTOResponse resultado = service.enviar(5L, new MensajeDTORequest("Hola Ana"));

        assertThat(resultado.emisorNombre(), is(equalTo("Pepe")));
    }

    @Test
    void testSePuedeEscribirEnUnaSolicitudCompletada() {
        solicitud(EstadoSolicitud.COMPLETADA);
        loguearComo("ana@test.com");
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MensajeDTOResponse resultado = service.enviar(5L, new MensajeDTORequest("Gracias"));

        assertThat(resultado.texto(), is(equalTo("Gracias")));
    }

    @Test
    void testUnaTerceraPersonaNoPuedeEscribirDa403() {
        solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("intruso@test.com");

        assertThrows(ChatExceptionAccesoDenegado.class,
                () -> service.enviar(5L, new MensajeDTORequest("Hola")));

        verify(repository, never()).save(ArgumentMatchers.any(MensajeEntity.class));
    }

    @Test
    void testEnUnaSolicitudRechazadaNoSePuedeEscribirDa409() {
        solicitud(EstadoSolicitud.RECHAZADA);
        loguearComo("ana@test.com");

        assertThrows(ChatExceptionConflicto.class,
                () -> service.enviar(5L, new MensajeDTORequest("Hola")));

        verify(repository, never()).save(ArgumentMatchers.any(MensajeEntity.class));
    }

    @Test
    void testEscribirEnUnaSolicitudQueNoExisteDa404() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class,
                () -> service.enviar(99L, new MensajeDTORequest("Hola")));
    }

    @Test
    void testLeerDevuelveTodosLosMensajesYMarcaLosMios() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("ana@test.com");
        when(repository.findBySolicitudIdOrderByIdAsc(5L)).thenReturn(List.of(
                mensaje(1L, ana, "Hola", solicitud),
                mensaje(2L, pepe, "Buenas", solicitud)));

        List<MensajeDTOResponse> resultado = service.getMensajes(5L, null);

        assertThat(resultado, hasSize(2));
        assertThat(resultado.get(0).mio(), is(equalTo(true)));
        assertThat(resultado.get(1).mio(), is(equalTo(false)));
    }

    @Test
    void testLeerConDesdeSoloDevuelveLosMasNuevos() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("pepe@test.com");
        when(repository.findBySolicitudIdAndIdGreaterThanOrderByIdAsc(5L, 1L))
                .thenReturn(List.of(mensaje(2L, ana, "Nuevo", solicitud)));

        List<MensajeDTOResponse> resultado = service.getMensajes(5L, 1L);

        assertThat(resultado, hasSize(1));
        assertThat(resultado.get(0).id(), is(equalTo(2L)));
        verify(repository, never()).findBySolicitudIdOrderByIdAsc(5L);
    }

    @Test
    void testUnaTerceraPersonaNoPuedeLeerDa403() {
        solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("intruso@test.com");

        assertThrows(ChatExceptionAccesoDenegado.class, () -> service.getMensajes(5L, null));

        verify(repository, never()).findBySolicitudIdOrderByIdAsc(5L);
    }

    @Test
    void testEnUnaSolicitudRechazadaSePuedeLeer() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.RECHAZADA);
        loguearComo("ana@test.com");
        when(repository.findBySolicitudIdOrderByIdAsc(5L))
                .thenReturn(List.of(mensaje(1L, ana, "Hola", solicitud)));

        assertThat(service.getMensajes(5L, null), hasSize(1));
    }

    @Test
    void testAlLeerSeMarcanComoLeidosLosMensajesDeLaOtraPersona() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("ana@test.com");
        MensajeEntity dePepe = mensaje(2L, pepe, "Buenas", solicitud);
        when(repository.findBySolicitudIdOrderByIdAsc(5L)).thenReturn(List.of(dePepe));
        when(repository.findBySolicitudIdAndEmisorIdNotAndLeidoFalse(5L, 1L)).thenReturn(List.of(dePepe));

        service.getMensajes(5L, null);

        assertThat(dePepe.isLeido(), is(true));
        verify(repository).saveAll(List.of(dePepe));
    }

    @Test
    void testAlLeerNoSeMarcanLosMensajesPropios() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("ana@test.com");
        MensajeEntity deAna = mensaje(1L, ana, "Hola", solicitud);
        when(repository.findBySolicitudIdOrderByIdAsc(5L)).thenReturn(List.of(deAna));
        // El repositorio no los devuelve como pendientes de Ana, porque los escribió ella
        when(repository.findBySolicitudIdAndEmisorIdNotAndLeidoFalse(5L, 1L)).thenReturn(List.of());

        service.getMensajes(5L, null);

        assertThat(deAna.isLeido(), is(false));
    }

    @Test
    void testUnaTerceraPersonaNoMarcaNadaComoLeido() {
        solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("intruso@test.com");

        assertThrows(ChatExceptionAccesoDenegado.class, () -> service.getMensajes(5L, null));

        verify(repository, never()).saveAll(ArgumentMatchers.anyList());
    }

    @Test
    void testNoLeidosCuentaElTotalYPorSolicitud() {
        SolicitudEntity cinco = SolicitudEntity.builder().id(5L).build();
        SolicitudEntity seis = SolicitudEntity.builder().id(6L).build();
        loguearComo("ana@test.com");
        when(repository.findNoLeidosDe("ana@test.com")).thenReturn(List.of(
                mensaje(1L, pepe, "a", cinco),
                mensaje(2L, pepe, "b", cinco),
                mensaje(3L, pepe, "c", seis)));

        MensajesNoLeidosDTOResponse resultado = service.getNoLeidos();

        assertThat(resultado.total(), is(equalTo(3L)));
        assertThat(resultado.porSolicitud().get(5L), is(equalTo(2L)));
        assertThat(resultado.porSolicitud().get(6L), is(equalTo(1L)));
    }

    @Test
    void testNoLeidosSinMensajesDevuelveCeroYMapaVacio() {
        loguearComo("ana@test.com");
        when(repository.findNoLeidosDe("ana@test.com")).thenReturn(List.of());

        MensajesNoLeidosDTOResponse resultado = service.getNoLeidos();

        assertThat(resultado.total(), is(equalTo(0L)));
        assertThat(resultado.porSolicitud().isEmpty(), is(true));
    }

    @Test
    void testElPrimerMensajeSinLeerAvisaPorCorreo() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.PENDIENTE);
        loguearComo("ana@test.com");
        when(repository.existsBySolicitudIdAndEmisorIdAndLeidoFalse(5L, 1L)).thenReturn(false);
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enviar(5L, new MensajeDTORequest("Hola Pepe"));

        verify(avisos).avisarMensajeNuevo(solicitud, ana);
    }

    @Test
    void testSiYaHabiaMensajesSinLeerNoSeVuelveAAvisar() {
        solicitud(EstadoSolicitud.PENDIENTE);
        loguearComo("ana@test.com");
        when(repository.existsBySolicitudIdAndEmisorIdAndLeidoFalse(5L, 1L)).thenReturn(true);
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enviar(5L, new MensajeDTORequest("Otro más"));

        verify(avisos, never()).avisarMensajeNuevo(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void testLaPersonaCuidadoraTambienAvisaALaFamilia() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("pepe@test.com");
        when(repository.save(ArgumentMatchers.any(MensajeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enviar(5L, new MensajeDTORequest("Hola Ana"));

        verify(avisos).avisarMensajeNuevo(solicitud, pepe);
    }

    @Test
    void testSiNoSePuedeEscribirNoSeAvisa() {
        solicitud(EstadoSolicitud.RECHAZADA);
        loguearComo("ana@test.com");

        assertThrows(ChatExceptionConflicto.class, () -> service.enviar(5L, new MensajeDTORequest("Hola")));

        verify(avisos, never()).avisarMensajeNuevo(ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
