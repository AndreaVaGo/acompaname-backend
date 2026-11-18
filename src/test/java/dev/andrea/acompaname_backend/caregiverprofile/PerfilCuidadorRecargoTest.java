package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// El recargo por persona extra propio de cada persona cuidadora: mapper y servicio
@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorRecargoTest {

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

    // Petición con el recargo que le pasemos (null = sin recargo propio)
    private PerfilCuidadorDTORequest crearRequest(Integer recargo) {
        return new PerfilCuidadorDTORequest("Pediatría", 6, new BigDecimal("22.00"), "Nueva bio actualizada", false,
                false, 1L, null, null, null, null, null, null, null, null, recargo);
    }

    @Test
    void testToEntityGuardaElRecargoPropio() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(30), crearUsuario());

        assertThat(perfil.getRecargoPersonaExtra(), is(equalTo(30)));
    }

    @Test
    void testToEntitySinRecargoPropioGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(null), crearUsuario());

        assertThat(perfil.getRecargoPersonaExtra(), is(nullValue()));
    }

    @Test
    void testToDTODevuelveElRecargoPropio() {
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setRecargoPersonaExtra(40);

        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfil);

        assertThat(dto.recargoPersonaExtra(), is(equalTo(40)));
    }

    @Test
    void testToDTOSinRecargoPropioDevuelveNull() {
        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(crearPerfil());

        assertThat(dto.recargoPersonaExtra(), is(nullValue()));
    }

    @Test
    void testUpdateGuardaElRecargoPropio() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("juan@test.com", null));
        PerfilCuidadorEntity perfil = crearPerfil();
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(35));

        assertThat(resultado.recargoPersonaExtra(), is(equalTo(35)));
    }

    @Test
    void testUpdateSinRecargoVuelveAlRecargoPorDefecto() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("juan@test.com", null));
        PerfilCuidadorEntity perfil = crearPerfil();
        perfil.setRecargoPersonaExtra(50);
        when(repository.findById(1L)).thenReturn(Optional.of(perfil));
        when(repository.save(Mockito.any(PerfilCuidadorEntity.class))).thenReturn(perfil);

        PerfilCuidadorDTOResponse resultado = service.update(1L, crearRequest(null));

        assertThat(resultado.recargoPersonaExtra(), is(nullValue()));
    }
}
