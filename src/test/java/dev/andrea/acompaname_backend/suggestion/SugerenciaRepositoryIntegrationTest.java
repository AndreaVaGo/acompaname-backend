package dev.andrea.acompaname_backend.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba el orden de las sugerencias contra un MySQL real (Testcontainers)
@DataJpaTest
@Testcontainers
public class SugerenciaRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private SugerenciaRepository repository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private UsuarioEntity crearUsuario(String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre("Ana")
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    private SugerenciaEntity guardar(UsuarioEntity usuario, String texto, LocalDateTime fecha) {
        SugerenciaEntity sugerencia = SugerenciaEntity.builder()
                .texto(texto)
                .fecha(fecha)
                .usuario(usuario)
                .build();
        return repository.saveAndFlush(sugerencia);
    }

    @Test
    void testSinSugerenciasDevuelveUnaListaVacia() {
        assertThat(repository.findAllByOrderByFechaDescIdDesc(), is(empty()));
    }

    @Test
    void testLasMasRecientesSalenPrimero() {
        UsuarioEntity ana = crearUsuario("ana.sug@test.com");
        guardar(ana, "Antigua", LocalDateTime.of(2026, 11, 10, 9, 0));
        guardar(ana, "Reciente", LocalDateTime.of(2026, 11, 17, 22, 0));
        guardar(ana, "Intermedia", LocalDateTime.of(2026, 11, 14, 12, 0));

        List<SugerenciaEntity> lista = repository.findAllByOrderByFechaDescIdDesc();

        assertThat(lista.size(), is(equalTo(3)));
        assertThat(lista.get(0).getTexto(), is(equalTo("Reciente")));
        assertThat(lista.get(1).getTexto(), is(equalTo("Intermedia")));
        assertThat(lista.get(2).getTexto(), is(equalTo("Antigua")));
    }

    @Test
    void testConLaMismaFechaSaleAntesLaUltimaGuardada() {
        UsuarioEntity ana = crearUsuario("ana.sug2@test.com");
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 17, 22, 0);
        guardar(ana, "Primera", fecha);
        guardar(ana, "Segunda", fecha);

        List<SugerenciaEntity> lista = repository.findAllByOrderByFechaDescIdDesc();

        assertThat(lista.get(0).getTexto(), is(equalTo("Segunda")));
        assertThat(lista.get(1).getTexto(), is(equalTo("Primera")));
    }

    @Test
    void testGuardaUnTextoDeMilCaracteres() {
        UsuarioEntity ana = crearUsuario("ana.sug3@test.com");

        SugerenciaEntity guardada = guardar(ana, "a".repeat(1000), LocalDateTime.now());

        assertThat(repository.findById(guardada.getId()).orElseThrow().getTexto().length(), is(equalTo(1000)));
    }
}
