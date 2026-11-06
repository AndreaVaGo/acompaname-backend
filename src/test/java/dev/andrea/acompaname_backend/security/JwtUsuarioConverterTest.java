package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la pieza que convierte el token en la persona de la base de datos
public class JwtUsuarioConverterTest {

    private final UsuarioRepository repository = Mockito.mock(UsuarioRepository.class);
    private final JwtUsuarioConverter converter = new JwtUsuarioConverter(repository);

    // Un token con el id 7 como sujeto
    private Jwt tokenDelUsuario7() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("7")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private UsuarioEntity ana(String rol) {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName(rol);
        return new UsuarioEntity(7L, "Ana", "ana@test.com", "600111222", "1234", Set.of(role));
    }

    @Test
    void testUnTokenValidDejaALaPersonaLogueadaConSuEmail() {
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(ana("FAMILIA")));

        Authentication auth = converter.convert(tokenDelUsuario7());

        assertThat(auth.getName(), is(equalTo("ana@test.com")));
        assertThat(auth.isAuthenticated(), is(equalTo(true)));
    }

    @Test
    void testLosRolesSeLeenDeLaBaseDeDatosYNoDelToken() {
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(ana("ADMIN")));

        Authentication auth = converter.convert(tokenDelUsuario7());

        assertThat(auth.getAuthorities().iterator().next().getAuthority(), is(equalTo("ADMIN")));
    }

    @Test
    void testElPrincipalEsUnSecurityUserConSuId() {
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(ana("FAMILIA")));

        Authentication auth = converter.convert(tokenDelUsuario7());

        SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
        assertThat(securityUser.getUsuario().getId(), is(equalTo(7L)));
    }

    @Test
    void testSiLaPersonaYaNoExisteElTokenSeRechaza() {
        Mockito.when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> converter.convert(tokenDelUsuario7()));
    }

    @Test
    void testSiLaPersonaEstaBloqueadaElTokenSeRechaza() {
        UsuarioEntity bloqueada = ana("FAMILIA");
        bloqueada.setActivo(false);
        Mockito.when(repository.findById(7L)).thenReturn(Optional.of(bloqueada));

        assertThrows(DisabledException.class, () -> converter.convert(tokenDelUsuario7()));
    }
}