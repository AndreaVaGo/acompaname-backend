package dev.andrea.acompaname_backend.solicitud;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;

// Tests de los casos de error y de seguridad de SolicitudServiceImpl:
// que no existe (404), que no es tuya (403) y que solo veo mis solicitudes.
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplErroresTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;

    private SolicitudServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        // Guardo el contexto de seguridad que hubiera y uso uno vacío,
        // para no afectar a los otros tests. Al terminar lo devuelvo.
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository);
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

    // Solicitud de Ana (familia) a Pepe (cuidador)
    private SolicitudEntity solicitudAnaPepe() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com", rol(1L, "FAMILIA"));
        UsuarioEntity pepe = usuario(2L, "Pepe", "pepe@test.com", rol(2L, "CUIDADOR"));
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, pepe);
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.PENDIENTE, ana, perfilPepe);
    }

    // Solicitud de Luis (familia) a Marta (cuidadora), que no tiene que ver con Ana ni con Pepe
    private SolicitudEntity solicitudLuisMarta() {
        UsuarioEntity luis = usuario(3L, "Luis", "luis@test.com", rol(1L, "FAMILIA"));
        UsuarioEntity marta = usuario(4L, "Marta", "marta@test.com", rol(2L, "CUIDADOR"));
        PerfilCuidadorEntity perfilMarta = new PerfilCuidadorEntity(2L, "Enfermería", 6, new BigDecimal("20.00"),
                "Bio", false, true, marta);
        return new SolicitudEntity(2L, "Hospital", "Carmen", "Sin notas", 75, LocalDate.of(2026, 9, 12),
                EstadoSolicitud.PENDIENTE, luis, perfilMarta);
    }

    private SolicitudDTORequest dtoCualquiera(Long familiaId, Long cuidadorId) {
        return new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                familiaId, cuidadorId);
    }

    @Test
    void testGetByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class, () -> service.getById(99L));
    }

    @Test
    void testGetByIdDeOtraPersonaDaAccesoDenegado() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.getById(1L));
    }

    @Test
    void testElCuidadorDeLaSolicitudPuedeVerla() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        SolicitudDTOResponse solicitud = service.getById(1L);

        assertThat(solicitud.tipoCuidado(), is(equalTo("Acompañamiento")));
    }

    @Test
    void testDeleteDeOtraPersonaDaAccesoDenegadoYNoBorra() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testUpdateDeOtraPersonaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(SolicitudExceptionAccesoDenegado.class, () -> service.update(1L, dtoCualquiera(1L, 1L)));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testCambiarEstadoDeOtraPersonaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(SolicitudExceptionAccesoDenegado.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConFamiliaQueNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.storeEntity(dtoCualquiera(99L, 1L)));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConCuidadorQueNoExiste() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com", rol(1L, "FAMILIA"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(perfilCuidadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.storeEntity(dtoCualquiera(1L, 99L)));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testMisSolicitudesSoloDevuelveLasMias() {
        loguearComo("ana@test.com");
        when(repository.findAll()).thenReturn(List.of(solicitudAnaPepe(), solicitudLuisMarta()));

        List<SolicitudDTOResponse> mias = service.getMisSolicitudes();

        assertThat(mias.size(), is(equalTo(1)));
        assertThat(mias.get(0).tipoCuidado(), is(equalTo("Acompañamiento")));
    }
}