package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Testcontainers
public class PerfilCuidadorRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private PerfilCuidadorRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testGuardaUnaFotoGrande() {
        String fotoGrande = "a".repeat(100000);
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .foto(fotoGrande)
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getFoto().length(), is(100000));
        assertThat(leido.getFoto(), is(equalTo(fotoGrande)));
    }

    @Test
    void testGuardaUnPerfilSinFoto() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .build();

        PerfilCuidadorEntity guardado = repository.saveAndFlush(perfil);
        entityManager.clear();

        PerfilCuidadorEntity leido = repository.findById(guardado.getId()).get();
        assertThat(leido.getFoto() == null, is(true));
    }
}