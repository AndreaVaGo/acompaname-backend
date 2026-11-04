package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de los cambios de estado prohibidos y de la edición solo en solicitudes pendientes.
// Qué transiciones son válidas se prueba en EstadoSolicitudTest; aquí, qué hace el servicio con ellas.
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplEstadosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;

    private SolicitudServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private UsuarioEntity usuario(Long id, String nombre, String email, RoleEntity rol) {
        return new UsuarioEntity(id, nombre, email, "600111222", "1234", Set.of(rol));
    }

    // Solicitud de Ana (familia) a Pepe (cuidador) en el estado que le pasemos
    private SolicitudEntity solicitudEn(EstadoSolicitud estado) {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com", rol(1L, "FAMILIA"));
        UsuarioEntity pepe = usuario(2L, "Pepe", "pepe@test.com", rol(2L, "CUIDADOR"));
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, pepe);
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                estado, ana, perfilPepe);
    }

    private SolicitudDTORequest dtoCualquiera() {
        return new SolicitudDTORequest("Hospital", "Manuel", "Notas nuevas", 80, LocalDate.of(2026, 9, 11),
                1L, 1L);
    }

    // ---- Cambios de estado ----

    @Test
    void testNoSePuedeCompletarUnaSolicitudPendienteYNoGuardaNiCreaPago() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.PENDIENTE)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
        verify(pagoRepository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSePuedeRechazarUnaSolicitudYaAceptada() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.ACEPTADA)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.RECHAZADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testNoSePuedeAceptarUnaSolicitudYaRechazada() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.RECHAZADA)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testNoSePuedeAceptarUnaSolicitudYaCompletada() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.COMPLETADA)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testNoSePuedeVolverAAceptarUnaSolicitudAceptadaYNoMiraSolapamientos() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.ACEPTADA)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA));
        verify(repository, never()).countSolapamientos(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), Mockito.any(EstadoSolicitud.class));
    }

    @Test
    void testNoSePuedeVolverAPendienteNingunaSolicitud() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.ACEPTADA)));

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.PENDIENTE));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testElMensajeDelErrorDiceDeQueEstadoANuevoEstado() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.RECHAZADA)));

        SolicitudExceptionEstadoInvalido error = assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA));

        assertThat(error.getMessage(), containsString("RECHAZADA"));
        assertThat(error.getMessage(), containsString("COMPLETADA"));
    }

    @Test
    void testElFlujoCompletoPendienteAceptadaCompletadaEsValido() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);
        SolicitudDTOResponse resultado = service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA);

        assertThat(resultado.estado(), is(equalTo(EstadoSolicitud.COMPLETADA)));
        verify(pagoRepository).save(Mockito.any(PagoEntity.class));
    }

    // ---- Edición ----

    @Test
    void testSePuedeEditarUnaSolicitudPendiente() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        SolicitudDTOResponse resultado = service.update(1L, dtoCualquiera());

        assertThat(resultado.tipoCuidado(), is(equalTo("Hospital")));
        assertThat(resultado.notas(), is(equalTo("Notas nuevas")));
        verify(repository).save(Mockito.any(SolicitudEntity.class));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitud.class, names = { "ACEPTADA", "RECHAZADA", "COMPLETADA" })
    void testNoSePuedeEditarUnaSolicitudQueYaNoEstaPendiente(EstadoSolicitud estado) {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(estado)));

        assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.update(1L, dtoCualquiera()));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }
}