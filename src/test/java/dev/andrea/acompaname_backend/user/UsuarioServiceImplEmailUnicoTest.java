package dev.andrea.acompaname_backend.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.user.dtos.ActualizarContactoDTO;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionEmailDuplicado;

// Si dos personas usan el mismo email a la vez, la comprobación del servicio no lo ve (los dos
// "no existe") y es la base de datos la que frena al segundo. Aquí se comprueba que ese error
// llega a la persona como el mismo 409 de siempre y no como un error genérico.
@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplEmailUnicoTest {

    @Mock
    private UsuarioRepository repository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AnonimizadorDeCuenta anonimizador;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(repository, roleRepository, perfilCuidadorRepository, passwordEncoder,
                anonimizador);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("a@test.com", null));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registroConEmailRepetidoDetectadoPorLaBaseDeDatosDa409() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        when(repository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("Clave#1234")).thenReturn("hash");
        when(repository.save(org.mockito.ArgumentMatchers.any(UsuarioEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "nuevo@test.com", "600111222", "Clave#1234", Set.of(1L));

        assertThrows(UsuarioExceptionEmailDuplicado.class, () -> service.storeEntity(dto));
    }

    @Test
    void actualizarConEmailRepetidoDetectadoPorLaBaseDeDatosDa409() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Ana", "a@test.com", "600111222", "hash", Set.of());
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.findByEmail("otro@test.com")).thenReturn(Optional.empty());
        when(repository.save(usuario)).thenReturn(usuario);
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(repository).flush();

        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "otro@test.com", "600111222", "Clave#1234", Set.of(1L));

        assertThrows(UsuarioExceptionEmailDuplicado.class, () -> service.update(1L, dto));
    }

    @Test
    void actualizarContactoConEmailRepetidoDetectadoPorLaBaseDeDatosDa409() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Ana", "a@test.com", "600111222", "hash", Set.of());
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.findByEmail("otro@test.com")).thenReturn(Optional.empty());
        when(repository.save(usuario)).thenReturn(usuario);
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(repository).flush();

        ActualizarContactoDTO dto = new ActualizarContactoDTO("Ana", "otro@test.com", "600111222");

        assertThrows(UsuarioExceptionEmailDuplicado.class, () -> service.actualizarContacto(1L, dto));
    }
}
