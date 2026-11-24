package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
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

    private UsuarioEntity guardar(String email, LocalDateTime fechaAlta) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre("Ana")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .fechaAlta(fechaAlta)
                .build();
        return repository.saveAndFlush(usuario);
    }

    @Test
    void testAlGuardarUnUsuarioNuevoSeRellenaLaFechaDeAlta() {
        UsuarioEntity usuario = guardar("nuevo@test.com", null);

        assertThat(usuario.getFechaAlta() != null, is(true));
    }

    @Test
    void testCuentaSoloLosUsuariosCreadosDespuesDeLaFecha() {
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        guardar("reciente@test.com", LocalDateTime.now().minusDays(2));
        guardar("antiguo@test.com", LocalDateTime.now().minusDays(20));

        long nuevos = repository.countByFechaAltaAfter(hace7Dias);

        assertThat(nuevos, is(equalTo(1L)));
    }

    @Test
    void testLosUsuariosAntiguosSinFechaNoCuentanComoNuevos() {
        // Una cuenta anterior a ACOM-243 tiene la fecha de alta vacía (NULL) en la base de datos
        UsuarioEntity antiguo = guardar("sinfecha@test.com", null);
        antiguo.setFechaAlta(null);
        repository.saveAndFlush(antiguo);

        long nuevos = repository.countByFechaAltaAfter(LocalDateTime.now().minusDays(7));

        assertThat(nuevos, is(equalTo(0L)));
    }
}
