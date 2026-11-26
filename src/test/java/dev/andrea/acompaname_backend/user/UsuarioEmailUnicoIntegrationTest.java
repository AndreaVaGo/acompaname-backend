package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Con MySQL de verdad: la base de datos no deja guardar dos usuarios con el mismo email,
// aunque la comprobación del código no se ejecute (dos registros a la vez).
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false" })
public class UsuarioEmailUnicoIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private UsuarioRepository repository;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void limpiar() {
        repository.findByEmail("unico-" + sufijo + "@test.com").ifPresent(repository::delete);
    }

    private UsuarioEntity usuario(String email) {
        return new UsuarioEntity(null, "Ana", email, "600111222", "hash", new HashSet<>());
    }

    @Test
    void noSePuedenGuardarDosUsuariosConElMismoEmail() {
        String email = "unico-" + sufijo + "@test.com";
        repository.saveAndFlush(usuario(email));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(usuario(email)));
        assertThat(repository.findByEmail(email).isPresent(), is(true));
    }

    @Test
    void mayusculasYMinusculasCuentanComoElMismoEmail() {
        repository.saveAndFlush(usuario("unico-" + sufijo + "@test.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(usuario("UNICO-" + sufijo + "@TEST.com")));
    }

    @Test
    void noSePuedeGuardarUnUsuarioSinEmail() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(usuario(null)));
        assertThat(repository.findByEmail("unico-" + sufijo + "@test.com").isPresent(), is(equalTo(false)));
    }
}
