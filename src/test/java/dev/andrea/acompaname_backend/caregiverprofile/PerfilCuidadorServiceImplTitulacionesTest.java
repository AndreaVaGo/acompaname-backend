package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.globals.Titulacion;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplTitulacionesTest {

    private PerfilCuidadorServiceImpl service;
    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ValoracionRepository valoracionRepository;

    @BeforeEach
    void setup() {
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository, valoracionRepository,
                Mockito.mock(FotoValidador.class));
    }

    // Limpiamos el usuario logueado para no afectar a otros tests
    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioEntity crearUsuario() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("CUIDADOR");
        return new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Set.of(rol));
    }

    private PerfilCuidadorEntity crearPerfil() {
        return new PerfilCuidadorEntity(1L, "Enfermeria", 3, new BigDecimal("20.00"), "Bio de prueba", true, false,
                crearUsuario());
    }

    // Deja a Juan como usuario logueado, que es el dueño del perfil
    private void loguearComoJuan() {
        Authentication auth = new UsernamePasswordAuthenticationToken("juan@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Petición de actualización con las titulaciones y las otras titulaciones que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<Titulacion> titulaciones, String otras) {
        return new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"), "Nueva bio actualizada", false,
                false, 1L, null, null, null, null, null, null, titulaciones, otras);
    }

    @Test
    void testGetByIdDevuelveLasTitulaciones() {
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setTitulaciones("ENFERMERIA,PRIMEROS_AUXILIOS");
        perfil.setOtrasTitulaciones("Curso de geriatría");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));

        PerfilCuidadorDTOResponse resultado = service.getById(1L);

        assertThat(resultado.titulaciones(), contains(Titulacion.ENFERMERIA, Titulacion.PRIMEROS_AUXILIOS));
        assertThat(resultado.otrasTitulaciones(), is(equalTo("Curso de geriatría")));
    }

    @Test
    void testGetByIdSinTitulacionesDevuelveListaVacia() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearPerfil()));

        PerfilCuidadorDTOResponse resultado = service.getById(1L);

        assertThat(resultado.titulaciones(), is(empty()));
        assertThat(resultado.otrasTitulaciones(), is(nullValue()));
    }

    @Test
    void testUpdateGuardaLasTitulacionesYLasOtras() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L,
                crearRequest(List.of(Titulacion.AUXILIAR_ENFERMERIA, Titulacion.ATENCION_DEPENDENCIA),
                        "Curso de movilizaciones"));

        assertThat(resultado.titulaciones(),
                contains(Titulacion.AUXILIAR_ENFERMERIA, Titulacion.ATENCION_DEPENDENCIA));
        assertThat(resultado.otrasTitulaciones(), is(equalTo("Curso de movilizaciones")));
    }

    @Test
    void testUpdateSinTitulacionesMantieneLasQueTenia() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setTitulaciones("ENFERMERIA");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(null, null));

        assertThat(resultado.titulaciones(), contains(Titulacion.ENFERMERIA));
    }

    @Test
    void testUpdateConListaVaciaBorraLasTitulaciones() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setTitulaciones("ENFERMERIA,PRIMEROS_AUXILIOS");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(List.of(), null));

        assertThat(resultado.titulaciones(), is(empty()));
    }

    @Test
    void testUpdateSinOtrasTitulacionesLasBorra() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setOtrasTitulaciones("Curso antiguo");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(null, ""));

        assertThat(resultado.otrasTitulaciones(), is(nullValue()));
    }
}
