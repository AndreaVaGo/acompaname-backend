package dev.andrea.acompaname_backend.perfilcuidador;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
import dev.andrea.acompaname_backend.valoracion.ValoracionRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplTest {
    @InjectMocks
    private PerfilCuidadorServiceImpl service;
    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ValoracionRepository valoracionRepository;

    @BeforeEach
    void setup() {
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository, valoracionRepository);
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
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
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
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

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
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

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

    @Test
    void testGetByIdDevuelveLaFoto() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilMock.setFoto("https://ejemplo.com/foto.jpg");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.foto(), is(equalTo("https://ejemplo.com/foto.jpg")));
    }

    @Test
    void testUpdateGuardaLaFoto() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, "https://ejemplo.com/nueva.jpg");

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.foto(), is(equalTo("https://ejemplo.com/nueva.jpg")));
    }

    @Test
    void testUpdateSinFotoQuitaLaFoto() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setFoto("https://ejemplo.com/foto.jpg");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.foto(), is(nullValue()));
    }

    @Test
    void testGetByIdDevuelveLaValoracionMediaRedondeada() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
        when(valoracionRepository.mediaPorCuidador(1L)).thenReturn(4.666666);
        when(valoracionRepository.countBySolicitudCuidadorId(1L)).thenReturn(3L);

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.valoracionMedia(), is(equalTo(4.7)));
        assertThat(perfil.numValoraciones(), is(equalTo(3L)));
    }

    @Test
    void testGetByIdSinValoracionesDevuelveMediaNula() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));
        when(valoracionRepository.mediaPorCuidador(1L)).thenReturn(null);
        when(valoracionRepository.countBySolicitudCuidadorId(1L)).thenReturn(0L);

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.valoracionMedia(), is(nullValue()));
        assertThat(perfil.numValoraciones(), is(equalTo(0L)));
    }

    @Test
    void testGetByIdSinTipoDeCuidadoDevuelveAmbos() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.tipoCuidado(), is(equalTo("ambos")));
    }

    @Test
    void testUpdateGuardaElTipoDeCuidado() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, "domicilio");

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.tipoCuidado(), is(equalTo("domicilio")));
    }

    @Test
    void testUpdateSinTipoDeCuidadoMantieneElQueTenia() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setTipoCuidado("hospitalario");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.tipoCuidado(), is(equalTo("hospitalario")));
    }

    @Test
    void testGetByIdSinServiciosDevuelveListaVacia() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.servicios().size(), is(equalTo(0)));
    }

    @Test
    void testGetByIdDevuelveLosServicios() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilMock.setServicios("movilidad,medicacion");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.servicios().size(), is(equalTo(2)));
        assertThat(perfil.servicios().get(0), is(equalTo("movilidad")));
        assertThat(perfil.servicios().get(1), is(equalTo("medicacion")));
    }

    @Test
    void testUpdateGuardaLosServicios() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, null, List.of("higiene", "nocturno"));

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.servicios().size(), is(equalTo(2)));
        assertThat(resultado.servicios().get(0), is(equalTo("higiene")));
        assertThat(resultado.servicios().get(1), is(equalTo("nocturno")));
    }

    @Test
    void testUpdateSinServiciosMantieneLosQueTenia() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setServicios("paliativos");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.servicios().size(), is(equalTo(1)));
        assertThat(resultado.servicios().get(0), is(equalTo("paliativos")));
    }

    @Test
    void testUpdateConListaVaciaBorraLosServicios() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setServicios("paliativos,higiene");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, null, List.of());

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.servicios().size(), is(equalTo(0)));
    }

    @Test
    void testGetByIdSinSexoDevuelveNulo() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.sexo(), is(nullValue()));
    }

    @Test
    void testGetByIdDevuelveElSexo() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilMock.setSexo("MUJER");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.sexo(), is(equalTo("MUJER")));
    }

    @Test
    void testUpdateGuardaElSexo() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, null, null, "HOMBRE");

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.sexo(), is(equalTo("HOMBRE")));
    }

    @Test
    void testUpdateSinSexoMantieneElQueTenia() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setSexo("MUJER");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.sexo(), is(equalTo("MUJER")));
    }

    @Test
    void testUpdateConSexoVacioBorraElSexo() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setSexo("MUJER");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, null, null, "");

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.sexo(), is(nullValue()));
    }

    @Test
    void testGetByIdDevuelveLaCiudad() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilMock = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilMock.setCiudad("Gijón");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilMock));

        PerfilCuidadorDTOResponse perfil = service.getById(1L);

        assertThat(perfil.ciudad(), is(equalTo("Gijón")));
    }

    @Test
    void testUpdateGuardaLaCiudad() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L, null, null, null, null, "Oviedo");

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.ciudad(), is(equalTo("Oviedo")));
    }

    @Test
    void testUpdateSinCiudadQuitaLaCiudad() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234",
                Set.of(crearRolMock()));
        PerfilCuidadorEntity perfilExistente = new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"),
                "Bio de prueba", true, false, usuario);
        perfilExistente.setCiudad("Gijón");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilExistente));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfilExistente);
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"),
                "Nueva bio actualizada", false, false, 1L);

        PerfilCuidadorDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.ciudad(), is(nullValue()));
    }
}