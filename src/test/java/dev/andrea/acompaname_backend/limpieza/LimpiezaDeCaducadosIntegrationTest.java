package dev.andrea.acompaname_backend.limpieza;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.auth.CodigoLoginEntity;
import dev.andrea.acompaname_backend.auth.CodigoLoginRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenEntity;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Con MySQL de verdad: la consulta de borrado funciona y solo se llevan lo caducado
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false" })
public class LimpiezaDeCaducadosIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private LimpiezaDeCaducados limpieza;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private CodigoLoginRepository codigoLoginRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UsuarioEntity usuario;

    @BeforeEach
    void crearUsuario() {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName("FAMILIA").orElseThrow();
        RoleEntity rol = new RoleEntity();
        rol.setId(deLaBaseDeDatos.getId());
        rol.setName(deLaBaseDeDatos.getName());
        usuario = usuarioRepository.save(UsuarioEntity.builder()
                .nombre("Ana")
                .email("ana.limpieza@test.com")
                .telefono("600111222")
                .password(passwordEncoder.encode("Secreta123"))
                .roles(Set.of(rol))
                .build());
    }

    @AfterEach
    void borrarTodo() {
        codigoLoginRepository.deleteAll();
        tokenRepository.deleteAll();
        usuarioRepository.delete(usuario);
    }

    private void token(String hash, LocalDateTime expiraEn) {
        tokenRepository.save(PasswordResetTokenEntity.builder()
                .tokenHash(hash).usuario(usuario).expiraEn(expiraEn).build());
    }

    private void codigo(String desafio, LocalDateTime expiraEn) {
        codigoLoginRepository.save(CodigoLoginEntity.builder()
                .desafioHash(desafio).codigoHash("c".repeat(64)).usuario(usuario).expiraEn(expiraEn).build());
    }

    @Test
    void borraLosTokensCaducadosYDejaLosVigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        token("a".repeat(64), ahora.minusMinutes(1));
        token("b".repeat(64), ahora.plusMinutes(30));

        limpieza.limpiar(ahora);

        assertThat(tokenRepository.findByTokenHash("a".repeat(64)).isPresent(), is(false));
        assertThat(tokenRepository.findByTokenHash("b".repeat(64)).isPresent(), is(true));
    }

    @Test
    void borraLosCodigosCaducadosYDejaLosVigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        codigo("a".repeat(64), ahora.minusMinutes(1));
        codigo("b".repeat(64), ahora.plusMinutes(5));

        limpieza.limpiar(ahora);

        assertThat(codigoLoginRepository.findByDesafioHash("a".repeat(64)).isPresent(), is(false));
        assertThat(codigoLoginRepository.findByDesafioHash("b".repeat(64)).isPresent(), is(true));
    }

    @Test
    void sinNadaCaducadoNoPasaNada() {
        LocalDateTime ahora = LocalDateTime.now();
        token("a".repeat(64), ahora.plusMinutes(30));

        limpieza.limpiar(ahora);

        assertThat(tokenRepository.count(), is(1L));
    }
}
