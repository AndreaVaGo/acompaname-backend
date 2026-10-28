package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;

// Comprueba contra un MySQL real que el grupo de edad de la solicitud se guarda y se lee bien
@DataJpaTest
@Testcontainers
public class SolicitudGrupoEdadIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private SolicitudRepository repository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity crearUsuario(String nombre, String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    private PerfilCuidadorEntity crearCuidador(UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Pediatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build();
        return perfilCuidadorRepository.saveAndFlush(perfil);
    }

    // Guarda una solicitud con el grupo de edad que le pasemos (puede ser null) y la vuelve a leer de la base de datos
    private SolicitudEntity guardarYLeer(GrupoEdad grupo, String emailFamilia, String emailCuidador) {
        UsuarioEntity familia = crearUsuario("Ana", emailFamilia);
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", emailCuidador));
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .grupoEdad(grupo)
                .familia(familia)
                .cuidador(cuidador)
                .build();
        SolicitudEntity guardada = repository.saveAndFlush(solicitud);
        entityManager.clear();
        return repository.findById(guardada.getId()).get();
    }

    @Test
    void testGuardaYLeeElGrupoDeEdad() {
        SolicitudEntity leida = guardarYLeer(GrupoEdad.DISCAPACIDAD_DEPENDENCIA, "ana1@test.com", "pepe1@test.com");

        assertThat(leida.getGrupoEdad(), is(equalTo(GrupoEdad.DISCAPACIDAD_DEPENDENCIA)));
    }

    @Test
    void testUnaSolicitudSinGrupoDeEdadLoGuardaComoNull() {
        SolicitudEntity leida = guardarYLeer(null, "ana2@test.com", "pepe2@test.com");

        assertThat(leida.getGrupoEdad(), is(nullValue()));
    }
}