package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.Optional;

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
public class PerfilCuidadorRepositoryIntegrationTest {

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

    private PerfilCuidadorEntity crearPerfil(UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build();
        return repository.saveAndFlush(perfil);
    }

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

    @Test
    void testExisteElPerfilDeUnUsuarioQueLoTiene() {
        UsuarioEntity pepe = crearUsuario("pepe@test.com");
        crearPerfil(pepe);

        assertThat(repository.existsByUsuarioId(pepe.getId()), is(true));
    }

    @Test
    void testNoExisteElPerfilDeUnUsuarioQueNoLoTiene() {
        UsuarioEntity pepe = crearUsuario("pepe@test.com");
        UsuarioEntity ana = crearUsuario("ana@test.com");
        crearPerfil(pepe);

        assertThat(repository.existsByUsuarioId(ana.getId()), is(false));
    }

    @Test
    void testEncuentraElPerfilPorElEmailDelUsuario() {
        UsuarioEntity pepe = crearUsuario("pepe@test.com");
        PerfilCuidadorEntity perfil = crearPerfil(pepe);
        crearPerfil(crearUsuario("marta@test.com"));

        Optional<PerfilCuidadorEntity> encontrado = repository.findByUsuarioEmail("pepe@test.com");

        assertThat(encontrado.isPresent(), is(true));
        assertThat(encontrado.get().getId(), is(equalTo(perfil.getId())));
    }

    @Test
    void testNoEncuentraPerfilSiElEmailNoTieneUno() {
        crearUsuario("ana@test.com");

        assertThat(repository.findByUsuarioEmail("ana@test.com").isPresent(), is(false));
    }
}