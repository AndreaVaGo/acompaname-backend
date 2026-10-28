package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
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
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplGruposEdadTest {

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

    // Petición de actualización con los grupos de edad que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<GrupoEdad> grupos) {
        return new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"), "Nueva bio actualizada", false,
                false, 1L, null, null, null, null, null, grupos);
    }

    @Test
    void testGetByIdDevuelveLosGruposDeEdad() {
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setGruposEdad("BEBES,NINOS");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));

        PerfilCuidadorDTOResponse resultado = service.getById(1L);

        assertThat(resultado.gruposEdad(), contains(GrupoEdad.BEBES, GrupoEdad.NINOS));
    }

    @Test
    void testGetByIdSinGruposDevuelveListaVacia() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearPerfil()));

        PerfilCuidadorDTOResponse resultado = service.getById(1L);

        assertThat(resultado.gruposEdad(), is(empty()));
    }

    @Test
    void testUpdateGuardaLosGruposDeEdad() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service
                .update(1L, crearRequest(List.of(GrupoEdad.ADOLESCENTES, GrupoEdad.ADULTOS)));

        assertThat(resultado.gruposEdad(), contains(GrupoEdad.ADOLESCENTES, GrupoEdad.ADULTOS));
    }

    @Test
    void testUpdateSinGruposMantieneLosQueTenia() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setGruposEdad("MAYORES");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(null));

        assertThat(resultado.gruposEdad(), contains(GrupoEdad.MAYORES));
    }

    @Test
    void testUpdateConListaVaciaBorraLosGrupos() {
        loguearComoJuan();
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setGruposEdad("MAYORES,ADULTOS");
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(List.of()));

        assertThat(resultado.gruposEdad(), is(empty()));
    }
}