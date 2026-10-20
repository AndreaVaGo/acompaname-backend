package dev.andrea.acompaname_backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;

@ExtendWith(MockitoExtension.class)
public class AdminInitializerTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer crear(String email, String password) {
        return new AdminInitializer(usuarioRepository, roleRepository, passwordEncoder, email, password);
    }

    private RoleEntity rolAdmin() {
        RoleEntity rol = new RoleEntity();
        rol.setId(3L);
        rol.setName("ADMIN");
        return rol;
    }

    @Test
    void testRunSinEmailNoCreaNada() {
        AdminInitializer initializer = crear("", "clave123");

        initializer.run();

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void testRunSinPasswordNoCreaNada() {
        AdminInitializer initializer = crear("admin@acompaname.es", "");

        initializer.run();

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void testRunSiElAdminYaExisteNoLoVuelveACrear() {
        when(usuarioRepository.findByEmail("admin@acompaname.es")).thenReturn(Optional.of(new UsuarioEntity()));
        AdminInitializer initializer = crear("admin@acompaname.es", "clave123");

        initializer.run();

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void testRunSinRolAdminNoCreaNada() {
        when(usuarioRepository.findByEmail("admin@acompaname.es")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
        AdminInitializer initializer = crear("admin@acompaname.es", "clave123");

        initializer.run();

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void testRunCreaElAdminConLaPasswordEncriptada() {
        when(usuarioRepository.findByEmail("admin@acompaname.es")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(rolAdmin()));
        when(passwordEncoder.encode("clave123")).thenReturn("clave-encriptada");
        AdminInitializer initializer = crear("admin@acompaname.es", "clave123");

        initializer.run();

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity guardado = captor.getValue();

        assertEquals("admin@acompaname.es", guardado.getEmail());
        assertEquals("clave-encriptada", guardado.getPassword());
        assertEquals(1, guardado.getRoles().size());
        assertTrue(guardado.getRoles().contains(rolAdmin()));
    }
}