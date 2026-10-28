package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Testcontainers
public class PerfilCuidadorGruposEdadIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private PerfilCuidadorRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity crearUsuario(String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre("Pepe")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void testGuardaYLeeLosGruposDeEdad() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Pediatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .gruposEdad("BEBES,NINOS")
                .usuario(crearUsuario("grupos@test.com"))
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getGruposEdad(), is(equalTo("BEBES,NINOS")));
    }

    @Test
    void testUnPerfilSinGruposLosGuardaComoNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(crearUsuario("singrupos@test.com"))
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getGruposEdad(), is(nullValue()));
    }
}