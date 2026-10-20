package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba las consultas de SolicitudRepository contra un MySQL real (Testcontainers),
// porque un test con mocks no puede comprobar un filtro que hace la base de datos.
@DataJpaTest
@Testcontainers
public class SolicitudRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private SolicitudRepository repository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

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
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build();
        return perfilCuidadorRepository.saveAndFlush(perfil);
    }

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return repository.saveAndFlush(solicitud);
    }

    @Test
    void testLaFamiliaVeSusSolicitudes() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("ana@test.com");

        assertThat(solicitudes.size(), is(equalTo(1)));
        assertThat(solicitudes.get(0).getId(), is(equalTo(solicitud.getId())));
    }

    @Test
    void testElCuidadorVeSusSolicitudes() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("pepe@test.com");

        assertThat(solicitudes.size(), is(equalTo(1)));
        assertThat(solicitudes.get(0).getId(), is(equalTo(solicitud.getId())));
    }

    @Test
    void testUnUsuarioAjenoNoVeNingunaSolicitud() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        crearUsuario("Intruso", "intruso@test.com");
        crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("intruso@test.com");

        assertThat(solicitudes.size(), is(equalTo(0)));
    }

    @Test
    void testCadaUsuarioSoloVeSusSolicitudesAunqueHayaOtras() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        SolicitudEntity deAnaYPepe = crearSolicitud(ana, crearCuidador(pepe));
        SolicitudEntity deLuisYMarta = crearSolicitud(luis, crearCuidador(marta));

        List<SolicitudEntity> deAna = repository.findByParticipante("ana@test.com");
        List<SolicitudEntity> deMarta = repository.findByParticipante("marta@test.com");

        assertThat(deAna.size(), is(equalTo(1)));
        assertThat(deAna.get(0).getId(), is(equalTo(deAnaYPepe.getId())));
        assertThat(deMarta.size(), is(equalTo(1)));
        assertThat(deMarta.get(0).getId(), is(equalTo(deLuisYMarta.getId())));
    }
}