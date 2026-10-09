package dev.andrea.acompaname_backend.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleInitializerTest {
    @Mock
    private RoleRepository repository;

    private RoleInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new RoleInitializer(repository);
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    @Test
    void testRunSinRolesCreaLosDos() {
        when(repository.findAll()).thenReturn(List.of());

        initializer.run();

        verify(repository, times(2)).save(any(RoleEntity.class));
    }

    @Test
    void testRunConLosRolesYaCreadosNoGuardaNada() {
        when(repository.findAll()).thenReturn(List.of(rol(1L, "FAMILIA"), rol(2L, "CUIDADOR")));

        initializer.run();

        verify(repository, never()).save(any(RoleEntity.class));
    }

    @Test
    void testRunSoloCreaElRolQueFalta() {
        when(repository.findAll()).thenReturn(List.of(rol(1L, "FAMILIA")));

        initializer.run();

        verify(repository, times(1)).save(any(RoleEntity.class));
    }
}