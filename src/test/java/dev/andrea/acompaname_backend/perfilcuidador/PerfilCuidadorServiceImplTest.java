package dev.andrea.acompaname_backend.perfilcuidador;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplTest {
    @InjectMocks
    private PerfilCuidadorServiceImpl service;
    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setup() {
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository);
    }

    private RoleEntity crearRolMock() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("CUIDADOR");
        return rol;
    }

    @Test
    void testGetEntities() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        List<PerfilCuidadorEntity> perfilesMock = List.of(
                new PerfilCuidadorEntity(1L, "Fisioterapia", 5, new BigDecimal("15.00"), "Bio de prueba", true, true,
                        usuario));
        when(repository.findAll()).thenReturn(perfilesMock);

        List<PerfilCuidadorDTOResponse> perfiles = service.getEntities();
        assertThat(perfiles.size(), is(equalTo(1)));
        assertThat(perfiles.get(0).especialidad(), is(equalTo("Fisioterapia")));
    }

    @Test
    void testGetById() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
        PerfilCuidadorDTOResponse perfil = service.getById(1L);
        assertThat(perfil.especialidad(), is(equalTo("Enfermeria")));
    }

    @Test
    void testStorePerfilCuidador() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                "Cuidadora con experiencia en geriatría", true, true, 1L);
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(
                new PerfilCuidadorEntity(1L, dto.especialidad(), dto.anosExperiencia(), dto.tarifaHora(), dto.bio(),
                        dto.tieneVehiculo(), dto.disponibleAhora(), usuario));
        PerfilCuidadorDTOResponse entity = service.storeEntity(dto);
        assertThat(entity.anosExperiencia(), is(equalTo(4)));
    }

    @Test
    void testDeleteById() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
        service.deleteById(1L);
        Mockito.verify(repository).deleteById(1L);
    }

    @Test
    void testUpdate() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);
        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);
        assertThat(resultado.especialidad(), is(equalTo("Pediatría")));
    }
}