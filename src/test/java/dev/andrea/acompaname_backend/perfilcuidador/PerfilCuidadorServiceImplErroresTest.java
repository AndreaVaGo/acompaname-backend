package dev.andrea.acompaname_backend.perfilcuidador;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;

// Tests de los casos de error y de seguridad de PerfilCuidadorServiceImpl:
// perfil que no existe (404), perfil de otro cuidador (403) y perfil duplicado.
@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplErroresTest {
    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private PerfilCuidadorServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        // Uso un contexto de seguridad vacío para no afectar a los otros tests
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private UsuarioEntity pepe() {
        RoleEntity rol = new RoleEntity();
        rol.setId(2L);
        rol.setName("CUIDADOR");
        return new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678", Set.of(rol));
    }

    private PerfilCuidadorEntity perfilDePepe() {
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe());
    }

    private PerfilCuidadorDTORequest dtoCualquiera(Long usuarioId) {
        return new PerfilCuidadorDTORequest("Enfermería", 6, new BigDecimal("20.00"), "Bio nueva", false, true,
                usuarioId);
    }

    @Test
    void testGetByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.getById(99L));
    }

    @Test
    void testUpdateDelPerfilDeOtroCuidadorDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilDePepe()));

        assertThrows(PerfilCuidadorExceptionAccesoDenegado.class, () -> service.update(1L, dtoCualquiera(2L)));
        verify(repository, never()).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testDeleteDelPerfilDeOtroCuidadorDaAccesoDenegadoYNoBorra() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(perfilDePepe()));

        assertThrows(PerfilCuidadorExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testNoSePuedeCrearUnSegundoPerfilParaElMismoUsuario() {
        when(repository.findAll()).thenReturn(List.of(perfilDePepe()));

        assertThrows(PerfilCuidadorExceptionAccesoDenegado.class, () -> service.storeEntity(dtoCualquiera(2L)));
        verify(repository, never()).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testStoreConUsuarioQueNoExiste() {
        when(repository.findAll()).thenReturn(List.of());
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioExceptionNotFound.class, () -> service.storeEntity(dtoCualquiera(99L)));
        verify(repository, never()).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testNoSePuedeCrearUnPerfilParaOtroUsuario() {
        loguearComo("intruso@test.com");
        when(repository.findAll()).thenReturn(List.of());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(pepe()));

        assertThrows(PerfilCuidadorExceptionAccesoDenegado.class, () -> service.storeEntity(dtoCualquiera(2L)));
        verify(repository, never()).save(Mockito.any(PerfilCuidadorEntity.class));
    }

    @Test
    void testMiPerfilDevuelveElMio() {
        loguearComo("pepe@test.com");
        when(repository.findAll()).thenReturn(List.of(perfilDePepe()));

        PerfilCuidadorDTOResponse perfil = service.getMiPerfil();

        assertThat(perfil.especialidad(), is(equalTo("Geriatría")));
    }

    @Test
    void testMiPerfilSinPerfilCreadoDaNoEncontrado() {
        loguearComo("ana@test.com");
        when(repository.findAll()).thenReturn(List.of(perfilDePepe()));

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.getMiPerfil());
    }
}