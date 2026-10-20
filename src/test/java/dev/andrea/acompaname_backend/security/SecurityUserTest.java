package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SecurityUserTest {

    private UsuarioEntity usuario;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setName("FAMILIA");

        usuario = UsuarioEntity.builder()
                .email("ana@test.com")
                .password("12345678")
                .roles(Set.of(role))
                .build();
        securityUser = new SecurityUser(usuario);
    }

    @Test
    void testDatosDelUsuario() {
        assertThat(securityUser.getUsername(), is(equalTo("ana@test.com")));
        assertThat(securityUser.getPassword(), is(equalTo("12345678")));
        assertThat(securityUser.getUsuario(), is(sameInstance(usuario)));
    }

    @Test
    void testAutoridadesSonLosRoles() {
        GrantedAuthority autoridad = securityUser.getAuthorities().iterator().next();

        assertThat(securityUser.getAuthorities().size(), is(1));
        assertThat(autoridad.getAuthority(), is(equalTo("FAMILIA")));
    }

    @Test
    void testLaCuentaEstaActiva() {
        assertThat(securityUser.isAccountNonExpired(), is(true));
        assertThat(securityUser.isAccountNonLocked(), is(true));
        assertThat(securityUser.isCredentialsNonExpired(), is(true));
        assertThat(securityUser.isEnabled(), is(true));
    }

    @Test
    void testUnUsuarioBloqueadoNoEstaHabilitado() {
        usuario.setActivo(false);

        assertThat(securityUser.isEnabled(), is(false));
    }
}