package dev.andrea.acompaname_backend.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {
    @Mock
    private RoleRepository repository;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(repository);
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    @Test
    void testIndexDevuelveLosRolesComoDTO() {
        when(repository.findAll()).thenReturn(List.of(rol(1L, "FAMILIA"), rol(2L, "CUIDADOR")));

        List<RoleDTOResponse> resultado = service.index();

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0), is(equalTo(new RoleDTOResponse(1L, "FAMILIA"))));
        assertThat(resultado.get(1), is(equalTo(new RoleDTOResponse(2L, "CUIDADOR"))));
    }

    @Test
    void testIndexSinRolesDevuelveListaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        List<RoleDTOResponse> resultado = service.index();

        assertThat(resultado, is(empty()));
    }

    @Test
    void testIndexNoDevuelveElRolAdmin() {
        when(repository.findAll())
                .thenReturn(List.of(rol(1L, "FAMILIA"), rol(2L, "CUIDADOR"), rol(3L, "ADMIN")));

        List<RoleDTOResponse> resultado = service.index();

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0), is(equalTo(new RoleDTOResponse(1L, "FAMILIA"))));
        assertThat(resultado.get(1), is(equalTo(new RoleDTOResponse(2L, "CUIDADOR"))));
    }
}