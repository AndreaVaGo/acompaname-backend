package dev.andrea.acompaname_backend.usuario;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplTest {
    @InjectMocks
    private UsuarioServiceImpl service;
    @Mock
    private UsuarioRepository repository;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(repository);
    }

    @Test
    void testGetEntities() {
        List<UsuarioEntity> usuariosMock = List.of(
                new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Rol.FAMILIA),
                new UsuarioEntity(2L, "Maria", "maria@test.com", "600333444", "5678", Rol.CUIDADOR));
        when(repository.findAll()).thenReturn(usuariosMock);

        List<UsuarioEntity> usuarios = service.getEntities();

        assertThat(usuarios.size(), is(equalTo(2)));
        assertThat(usuarios.get(0).getNombre(), is(equalTo("Juan")));
        assertThat(usuarios.get(1).getNombre(), is(equalTo("Maria")));
    }

    @Test
    void testGetById() {
        UsuarioEntity usuarioMock = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Rol.FAMILIA);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        UsuarioEntity usuario = service.getById(1L);

        assertThat(usuario.getNombre(), is(equalTo("Juan")));
        assertThat(usuario.getEmail(), is(equalTo("juan@test.com")));
    }

    @Test
    void testStoreUsuario() {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "1234", Rol.FAMILIA);
        when(repository.save(Mockito.any(UsuarioEntity.class))).thenReturn(
                new UsuarioEntity(1L, dto.nombre(), dto.email(), dto.telefono(), dto.password(), dto.rol()));
        UsuarioDTOResponse entity = service.storeEntity(dto);
        assertThat(entity.nombre(), is(equalTo("Ana")));
    }

    @Test
    void testDeleteById() {
        service.deleteById(1L);
        Mockito.verify(repository).deleteById(1L);
    }

    @Test
    void testUpdate() {
        UsuarioEntity usuarioExistente = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Rol.FAMILIA);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(repository.save(Mockito.any(UsuarioEntity.class))).thenReturn(usuarioExistente);
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "1234", Rol.FAMILIA);
        UsuarioDTOResponse resultado = service.update(1L, dto);
        assertThat(resultado.nombre(), is(equalTo("Ana")));

    }
}
