package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real (Testcontainers) la consulta que filtra las solicitudes por estado
@DataJpaTest
@Testcontainers
public class AdminSolicitudesIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;

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

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador,
            EstadoSolicitud estado) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(estado)
                .fechaCuidado(LocalDate.of(2026, 11, 1))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return solicitudRepository.saveAndFlush(solicitud);
    }

    @Test
    void testFindByEstadoDevuelveSoloLasSolicitudesDeEseEstado() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE);
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE);
        crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA);

        List<SolicitudEntity> pendientes = solicitudRepository.findByEstado(EstadoSolicitud.PENDIENTE);

        assertThat(pendientes.size(), is(equalTo(2)));
        assertThat(pendientes.get(0).getEstado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        assertThat(pendientes.get(1).getEstado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
    }

    @Test
    void testFindByEstadoSinCoincidenciasDevuelveListaVacia() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE);

        List<SolicitudEntity> rechazadas = solicitudRepository.findByEstado(EstadoSolicitud.RECHAZADA);

        assertThat(rechazadas, is(empty()));
    }
}