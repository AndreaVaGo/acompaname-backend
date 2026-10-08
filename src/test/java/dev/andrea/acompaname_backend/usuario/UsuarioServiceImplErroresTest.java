package dev.andrea.acompaname_backend.usuario;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;

// Tests de los casos de error y de seguridad de UsuarioServiceImpl:
// email repetido (409), usuario que no existe (404) y datos de otra persona (403).
@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplErroresTest {
    @Mock
    private UsuarioRepository repository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        // Uso un contexto de seguridad vacío para no afectar a los otros tests
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new UsuarioServiceImpl(repository, roleRepository, perfilCuidadorRepository, passwordEncoder);
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

    private UsuarioEntity juan() {
        return new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Set.of(rol(1L, "FAMILIA")));
    }

    private UsuarioDTORequest dtoAna(Long rolId) {
        return new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "12345678", Set.of(rolId));
    }

    @Test
    void testRegistroConEmailRepetidoDaConflictoYNoGuarda() {
        when(repository.findByEmail("ana@test.com")).thenReturn(Optional.of(juan()));

        assertThrows(UsuarioExceptionEmailDuplicado.class, () -> service.storeEntity(dtoAna(1L)));
        verify(repository, never()).save(Mockito.any(UsuarioEntity.class));
    }

    @Test
    void testRegistroDeCuidadorCreaSuPerfilVacio() {
        RoleEntity rolCuidador = rol(2L, "CUIDADOR");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(rolCuidador));
        when(passwordEncoder.encode("12345678")).thenReturn("encriptada123");
        when(repository.save(Mockito.any(UsuarioEntity.class))).thenReturn(
                new UsuarioEntity(5L, "Ana", "ana@test.com", "600555666", "encriptada123", Set.of(rolCuidador)));

        UsuarioDTOResponse usuario = service.storeEntity(dtoAna(2L));

        assertThat(usuario.nombre(), is(equalTo("Ana")));
        verify(perfilCuidadorRepository).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testRegistroDeFamiliaNoCreaPerfilDeCuidador() {
        RoleEntity rolFamilia = rol(1L, "FAMILIA");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(rolFamilia));
        when(passwordEncoder.encode("12345678")).thenReturn("encriptada123");
        when(repository.save(Mockito.any(UsuarioEntity.class))).thenReturn(
                new UsuarioEntity(5L, "Ana", "ana@test.com", "600555666", "encriptada123", Set.of(rolFamilia)));

        service.storeEntity(dtoAna(1L));

        verify(perfilCuidadorRepository, never()).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testRegistroConRolQueNoExisteDaNotFoundYNoGuarda() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.storeEntity(dtoAna(99L)));
        verify(repository, never()).save(Mockito.any(UsuarioEntity.class));
    }

    @Test
    void testUpdateConRolQueNoExisteDaNotFoundYNoGuarda() {
        loguearComo("juan@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(juan()));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.update(1L, dtoAna(99L)));
        verify(repository, never()).save(Mockito.any(UsuarioEntity.class));
    }

    @Test
    void testGetByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.getById(99L));
    }

    @Test
    void testGetByIdDeOtroUsuarioDaAccesoDenegado() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(juan()));

        assertThrows(UsuarioExceptionAccesoDenegado.class, () -> service.getById(1L));
    }

    @Test
    void testDeleteDeOtroUsuarioDaAccesoDenegadoYNoBorra() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(juan()));

        assertThrows(UsuarioExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testUpdateDeOtroUsuarioDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(juan()));

        assertThrows(UsuarioExceptionAccesoDenegado.class, () -> service.update(1L, dtoAna(1L)));
        verify(repository, never()).save(Mockito.any(UsuarioEntity.class));
    }
}