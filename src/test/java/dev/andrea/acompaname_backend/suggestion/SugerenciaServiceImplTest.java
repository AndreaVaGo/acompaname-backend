package dev.andrea.acompaname_backend.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;

@ExtendWith(MockitoExtension.class)
public class SugerenciaServiceImplTest {

    @Mock
    private SugerenciaRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private SugerenciaServiceImpl service;

    @BeforeEach
    void setup() {
        service = new SugerenciaServiceImpl(repository, usuarioRepository);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private UsuarioEntity crearUsuario(String email, String nombreRol) {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName(nombreRol);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(3L);
        usuario.setNombre("Ana");
        usuario.setEmail(email);
        usuario.setRoles(Set.of(rol));
        return usuario;
    }

    @Test
    void testCrearGuardaLaSugerenciaConLaPersonaLogueada() {
        loguearComo("ana@test.com");
        UsuarioEntity ana = crearUsuario("ana@test.com", "FAMILIA");
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));
        when(repository.save(ArgumentMatchers.any(SugerenciaEntity.class))).thenAnswer(invocation -> {
            SugerenciaEntity guardada = invocation.getArgument(0);
            guardada.setId(9L);
            return guardada;
        });

        SugerenciaDTOResponse respuesta = service.crear(new SugerenciaDTORequest("  Más colores  "));

        ArgumentCaptor<SugerenciaEntity> captor = ArgumentCaptor.forClass(SugerenciaEntity.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsuario(), is(equalTo(ana)));
        assertThat(captor.getValue().getTexto(), is(equalTo("Más colores")));
        assertThat(respuesta.id(), is(equalTo(9L)));
        assertThat(respuesta.texto(), is(equalTo("Más colores")));
    }

    @Test
    void testCrearConUnEmailQueNoExisteLanzaNotFoundYNoGuarda() {
        loguearComo("nadie@test.com");
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.crear(new SugerenciaDTORequest("Hola")));

        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void testListarDevuelveLasSugerenciasConSuAutor() {
        UsuarioEntity ana = crearUsuario("ana@test.com", "FAMILIA");
        SugerenciaEntity nueva = new SugerenciaEntity(2L, "Segunda", LocalDateTime.of(2026, 11, 17, 22, 0), ana);
        SugerenciaEntity vieja = new SugerenciaEntity(1L, "Primera", LocalDateTime.of(2026, 11, 16, 10, 0), ana);
        when(repository.findAllByOrderByFechaDescIdDesc()).thenReturn(List.of(nueva, vieja));

        List<AdminSugerenciaDTOResponse> lista = service.listar();

        assertThat(lista.size(), is(equalTo(2)));
        assertThat(lista.get(0).texto(), is(equalTo("Segunda")));
        assertThat(lista.get(0).emailUsuario(), is(equalTo("ana@test.com")));
        assertThat(lista.get(1).texto(), is(equalTo("Primera")));
    }

    @Test
    void testListarSinSugerenciasDevuelveUnaListaVacia() {
        when(repository.findAllByOrderByFechaDescIdDesc()).thenReturn(List.of());

        assertThat(service.listar().size(), is(equalTo(0)));
    }
}
