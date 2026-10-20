package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.admin.dtos.AdminSolicitudDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminUsuarioDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;

// Tests de los usuarios y las solicitudes en el panel de administración: listar, bloquear y desbloquear
@ExtendWith(MockitoExtension.class)
public class AdminServiceImplUsuariosTest {
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
        // Simulamos que el administrador ha iniciado sesión
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin@acompaname.com", null));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioEntity usuario(Long id, String nombre, String email, String nombreRol, boolean activo) {
        RoleEntity rol = new RoleEntity();
        rol.setName(nombreRol);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setTelefono("600000000");
        usuario.setRoles(Set.of(rol));
        usuario.setActivo(activo);
        return usuario;
    }

    private SolicitudEntity solicitud(Long id, EstadoSolicitud estado, String nombreFamilia, String nombreCuidador) {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setNombre(nombreFamilia);
        UsuarioEntity usuarioCuidador = new UsuarioEntity();
        usuarioCuidador.setNombre(nombreCuidador);
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity();
        cuidador.setUsuario(usuarioCuidador);

        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setId(id);
        solicitud.setTipoCuidado("domicilio");
        solicitud.setNombrePaciente("Abuela " + id);
        solicitud.setFechaCuidado(LocalDate.of(2026, 11, 20));
        solicitud.setEstado(estado);
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        return solicitud;
    }

    @Test
    void testSinUsuariosLaListaEstaVacia() {
        List<AdminUsuarioDTOResponse> resultado = service.usuarios();

        assertThat(resultado, is(empty()));
    }

    @Test
    void testLaListaDeUsuariosIncluyeRolYSiEstanActivos() {
        when(usuarioRepository.findAll()).thenReturn(List.of(
                usuario(1L, "Ana", "ana@mail.com", "FAMILIA", true),
                usuario(2L, "Pepe", "pepe@mail.com", "CUIDADOR", false)));

        List<AdminUsuarioDTOResponse> resultado = service.usuarios();

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).id(), is(equalTo(1L)));
        assertThat(resultado.get(0).nombre(), is(equalTo("Ana")));
        assertThat(resultado.get(0).email(), is(equalTo("ana@mail.com")));
        assertThat(resultado.get(0).rol(), is(equalTo("FAMILIA")));
        assertThat(resultado.get(0).activo(), is(true));
        assertThat(resultado.get(1).rol(), is(equalTo("CUIDADOR")));
        assertThat(resultado.get(1).activo(), is(false));
    }

    @Test
    void testBloquearUnUsuarioLoDejaInactivoYLoGuarda() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@mail.com", "FAMILIA", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(usuarioRepository.save(ana)).thenReturn(ana);

        AdminUsuarioDTOResponse resultado = service.cambiarActivo(1L, false);

        assertThat(resultado.activo(), is(false));
        assertThat(ana.isActivo(), is(false));
        verify(usuarioRepository).save(ana);
    }

    @Test
    void testDesbloquearUnUsuarioLoDejaActivoYLoGuarda() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@mail.com", "FAMILIA", false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(usuarioRepository.save(ana)).thenReturn(ana);

        AdminUsuarioDTOResponse resultado = service.cambiarActivo(1L, true);

        assertThat(resultado.activo(), is(true));
        assertThat(ana.isActivo(), is(true));
        verify(usuarioRepository).save(ana);
    }

    @Test
    void testBloquearUnUsuarioQueNoExisteDaNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.cambiarActivo(99L, false));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testElAdministradorNoPuedeBloquearseASiMismo() {
        UsuarioEntity admin = usuario(5L, "Admin", "admin@acompaname.com", "ADMIN", true);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(admin));

        assertThrows(UsuarioExceptionAccesoDenegado.class, () -> service.cambiarActivo(5L, false));

        assertThat(admin.isActivo(), is(true));
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testSolicitudesSinEstadoDevuelveTodas() {
        when(solicitudRepository.findAll()).thenReturn(List.of(
                solicitud(1L, EstadoSolicitud.PENDIENTE, "Ana", "Pepe"),
                solicitud(2L, EstadoSolicitud.ACEPTADA, "Luis", "Marta")));

        List<AdminSolicitudDTOResponse> resultado = service.solicitudes(null);

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).id(), is(equalTo(1L)));
        assertThat(resultado.get(0).nombrePaciente(), is(equalTo("Abuela 1")));
        assertThat(resultado.get(0).estado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        assertThat(resultado.get(0).nombreFamilia(), is(equalTo("Ana")));
        assertThat(resultado.get(0).nombreCuidador(), is(equalTo("Pepe")));
        assertThat(resultado.get(1).estado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
        verify(solicitudRepository, never()).findByEstado(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testSolicitudesConEstadoSoloBuscaEsaClase() {
        when(solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE))
                .thenReturn(List.of(solicitud(1L, EstadoSolicitud.PENDIENTE, "Ana", "Pepe")));

        List<AdminSolicitudDTOResponse> resultado = service.solicitudes(EstadoSolicitud.PENDIENTE);

        assertThat(resultado.size(), is(equalTo(1)));
        assertThat(resultado.get(0).estado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        verify(solicitudRepository, never()).findAll();
    }
}