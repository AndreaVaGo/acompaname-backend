package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;

// Prueba contra un MySQL real que los tokens se buscan por su hash, el hash no se repite
// y al borrar a una persona se borran sus tokens.
@DataJpaTest
public class PasswordResetTokenRepositoryIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private PasswordResetTokenRepository repository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity crearUsuario(String email) {
        return usuarioRepository.saveAndFlush(UsuarioEntity.builder()
                .nombre("Ana")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build());
    }

    private PasswordResetTokenEntity crearToken(UsuarioEntity usuario, String hash) {
        return repository.saveAndFlush(PasswordResetTokenEntity.builder()
                .tokenHash(hash)
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(60))
                .build());
    }

    @Test
    void testSeEncuentraPorSuHash() {
        UsuarioEntity ana = crearUsuario("ana.token@test.com");
        crearToken(ana, "1".repeat(64));

        Optional<PasswordResetTokenEntity> encontrado = repository.findByTokenHash("1".repeat(64));

        assertThat(encontrado.isPresent(), is(true));
        assertThat(encontrado.get().getUsuario().getId(), is(equalTo(ana.getId())));
    }

    @Test
    void testUnHashQueNoExisteNoDevuelveNada() {
        crearToken(crearUsuario("ana.nada@test.com"), "2".repeat(64));

        assertThat(repository.findByTokenHash("3".repeat(64)).isPresent(), is(false));
    }

    @Test
    void testNoSePuedenGuardarDosTokensConElMismoHash() {
        crearToken(crearUsuario("ana.uno@test.com"), "4".repeat(64));
        UsuarioEntity otra = crearUsuario("ana.dos@test.com");

        assertThrows(DataIntegrityViolationException.class, () -> crearToken(otra, "4".repeat(64)));
    }

    @Test
    void testGuardaLaCaducidad() {
        UsuarioEntity ana = crearUsuario("ana.caducidad@test.com");
        LocalDateTime caducidad = LocalDateTime.of(2030, 1, 15, 9, 30);
        repository.saveAndFlush(PasswordResetTokenEntity.builder()
                .tokenHash("5".repeat(64))
                .usuario(ana)
                .expiraEn(caducidad)
                .build());
        entityManager.clear();

        PasswordResetTokenEntity leido = repository.findByTokenHash("5".repeat(64)).orElseThrow();

        assertThat(leido.getExpiraEn(), is(equalTo(caducidad)));
    }

    @Test
    void testBorrarLosTokensDeUnaPersonaNoTocaLosDeOtra() {
        UsuarioEntity ana = crearUsuario("ana.borrar@test.com");
        UsuarioEntity luis = crearUsuario("luis.borrar@test.com");
        crearToken(ana, "6".repeat(64));
        crearToken(ana, "7".repeat(64));
        crearToken(luis, "8".repeat(64));

        repository.deleteByUsuarioId(ana.getId());
        repository.flush();

        assertThat(repository.findByTokenHash("6".repeat(64)).isPresent(), is(false));
        assertThat(repository.findByTokenHash("7".repeat(64)).isPresent(), is(false));
        assertThat(repository.findByTokenHash("8".repeat(64)).isPresent(), is(true));
    }

    @Test
    void testAlBorrarUnaPersonaSeBorranSusTokens() {
        UsuarioEntity ana = crearUsuario("ana.cascada@test.com");
        crearToken(ana, "9".repeat(64));
        entityManager.clear();

        usuarioRepository.deleteById(ana.getId());
        usuarioRepository.flush();
        entityManager.clear();

        assertThat(repository.findByTokenHash("9".repeat(64)).isPresent(), is(false));
    }
}
