package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de las fechas que guarda la solicitud: cuándo se crea y cuándo se acepta
@ExtendWith(MockitoExtension.class)
public class SolicitudFechasTest {
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
        // Guardo el contexto de seguridad que hubiera y uso uno vacío,
        // para no afectar a los otros tests. Al terminar lo devuelvo.
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

    private UsuarioEntity ana() {
        return new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol(1L, "FAMILIA")));
    }

    private PerfilCuidadorEntity perfilPepe() {
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe);
    }

    private SolicitudEntity solicitudPendiente() {
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.PENDIENTE, ana(), perfilPepe());
    }

    @Test
    void testUnaSolicitudNuevaTieneFechaDeCreacionYNoDeAceptacion() {
        SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), 1L, 1L);

        SolicitudEntity solicitud = SolicitudMapper.toEntity(dto, ana(), perfilPepe());

        assertThat(solicitud.getFechaCreacion(), is(notNullValue()));
        assertThat(solicitud.getFechaAceptacion(), is(nullValue()));
    }

    @Test
    void testAceptarLaSolicitudGuardaLaFechaDeAceptacion() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudPendiente();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

        assertThat(solicitud.getFechaAceptacion(), is(notNullValue()));
    }

    @Test
    void testRechazarLaSolicitudNoGuardaFechaDeAceptacion() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudPendiente();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.RECHAZADA);

        assertThat(solicitud.getFechaAceptacion(), is(nullValue()));
    }
}