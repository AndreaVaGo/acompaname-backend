package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;

// Prueba contra un MySQL real que el administrador se crea al arrancar la aplicación.
// Al arrancar no hay una transacción abierta, por eso el test tampoco abre una (NOT_SUPPORTED).
@DataJpaTest
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(AdminInitializer.class)
@TestPropertySource(properties = { "admin.email=admin@test.com", "admin.password=clave12345" })
public class AdminInitializerIntegrationTest {

    @TestConfiguration
    static class Configuracion {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private CommandLineRunner runner;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void prepararBaseDeDatos() {
        usuarioRepository.deleteAll();
        roleRepository.deleteAll();
        RoleEntity rolAdmin = new RoleEntity();
        rolAdmin.setName("ADMIN");
        roleRepository.saveAndFlush(rolAdmin);
    }

    @Test
    void testAlArrancarSeCreaElAdministradorConSuRol() throws Exception {
        runner.run();

        assertThat(usuarioRepository.findByEmail("admin@test.com").isPresent(), is(true));
        assertThat(usuarioRepository.countByRolesName("ADMIN"), is(equalTo(1L)));
    }

    @Test
    void testSiYaExisteNoSeCreaOtroAdministrador() throws Exception {
        runner.run();
        runner.run();

        assertThat(usuarioRepository.countByRolesName("ADMIN"), is(equalTo(1L)));
    }
}