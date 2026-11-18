package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;

@DataJpaTest
public class UsuarioRepositoryIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void testFindByEmailNotExists() {
        Optional<UsuarioEntity> usuario = repository.findByEmail("noexiste@test.com");
        assertThat(usuario.isPresent(), is(false));
    }
}