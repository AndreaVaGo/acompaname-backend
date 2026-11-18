package dev.andrea.acompaname_backend.servicerequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Comprueba cuándo el servicio de solicitudes manda los avisos por correo
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplAvisosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private BloqueOcupadoRepository bloqueRepository;
    @Mock
    private AvisosDeSolicitud avisos;

    private SolicitudServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository, bloqueRepository);
        service.setAvisos(avisos);
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

    private UsuarioEntity ana() {
        return new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol(1L, "FAMILIA")));
    }

    private PerfilCuidadorEntity perfilPepe() {
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe);
    }

    private SolicitudEntity solicitudEn(EstadoSolicitud estado) {
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                estado, ana(), perfilPepe());
    }

    // ---- Solicitud nueva ----

    @Test
    void testAlCrearUnaSolicitudSeAvisaAlCuidador() {
        loguearComo("ana@test.com");
        UsuarioEntity familia = ana();
        PerfilCuidadorEntity cuidador = perfilPepe();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(familia));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(cuidador));
        SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), 1L, 1L);
        SolicitudEntity guardada = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(guardada);

        service.storeEntity(dto);

        verify(avisos).avisarSolicitudNueva(guardada);
    }

    @Test
    void testSiLaSolicitudNoSeCreaNoSeAvisaANadie() {
        loguearComo("otra@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));
        SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), 1L, 1L);

        try {
            service.storeEntity(dto);
        } catch (RuntimeException e) {
            // Se espera el error de acceso denegado: lo que importa es que no se avisó a nadie
        }

        verifyNoInteractions(avisos);
    }

    // ---- Aceptar, rechazar y completar ----

    @Test
    void testAlAceptarSeAvisaALaFamiliaYSoloDeLaAceptacion() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

        verify(avisos).avisarSolicitudAceptada(solicitud);
        verify(avisos, never()).avisarSolicitudRechazada(Mockito.any());
        verify(avisos, never()).avisarSolicitudNueva(Mockito.any());
    }

    @Test
    void testAlRechazarSeAvisaALaFamiliaYSoloDelRechazo() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.RECHAZADA);

        verify(avisos).avisarSolicitudRechazada(solicitud);
        verify(avisos, never()).avisarSolicitudAceptada(Mockito.any());
    }

    @Test
    void testAlCompletarSoloSeAvisaDelServicioCompletado() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.ACEPTADA);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);
        when(pagoRepository.existsBySolicitudId(1L)).thenReturn(false);
        when(pagoRepository.save(Mockito.any(PagoEntity.class))).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA);

        verify(avisos).avisarServicioCompletado(solicitud, new BigDecimal("18.00"));
        verify(avisos, never()).avisarSolicitudAceptada(Mockito.any());
        verify(avisos, never()).avisarSolicitudRechazada(Mockito.any());
    }

    @Test
    void testSiElPagoYaExistiaAlCompletarNoSeAvisaOtraVez() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.ACEPTADA);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);
        when(pagoRepository.existsBySolicitudId(1L)).thenReturn(true);

        service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA);

        verifyNoInteractions(avisos);
    }

    @Test
    void testSiElCambioDeEstadoNoEsValidoNoSeAvisaANadie() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.RECHAZADA)));

        try {
            service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);
        } catch (SolicitudExceptionEstadoInvalido e) {
            // Se espera este error: lo que importa es que no se avisó a nadie
        }

        verifyNoInteractions(avisos);
    }

    // ---- Sin avisos configurados ----

    @Test
    void testSinAvisosLaSolicitudSigueFuncionandoIgual() {
        SolicitudServiceImpl sinAvisos = new SolicitudServiceImpl(repository, usuarioRepository,
                perfilCuidadorRepository, pagoRepository, bloqueRepository);
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudEn(EstadoSolicitud.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(solicitud)).thenReturn(solicitud);

        assertDoesNotThrow(() -> sinAvisos.cambiarEstado(1L, EstadoSolicitud.RECHAZADA));
    }
}
