package dev.andrea.acompaname_backend.admin;

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
import dev.andrea.acompaname_backend.review.ValoracionEntity;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real (Testcontainers) la consulta de las cinco últimas valoraciones:
// el orden y el límite de cinco los hace la base de datos.
@DataJpaTest
@Testcontainers
public class AdminValoracionesIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private ValoracionRepository valoracionRepository;

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

    // Cada valoración es de una solicitud distinta (una solicitud solo puede tener una valoración)
    private ValoracionEntity crearValoracion(UsuarioEntity familia, PerfilCuidadorEntity cuidador,
            LocalDate fecha) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.COMPLETADA)
                .fechaCuidado(LocalDate.of(2026, 11, 1))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        solicitudRepository.saveAndFlush(solicitud);
        ValoracionEntity valoracion = ValoracionEntity.builder()
                .comentario("Muy bien")
                .puntuacion(5)
                .fecha(fecha)
                .solicitud(solicitud)
                .build();
        return valoracionRepository.saveAndFlush(valoracion);
    }

    @Test
    void testDevuelveSoloLasCincoMasRecientesDeLaMasNuevaALaMasAntigua() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        for (int dia = 1; dia <= 6; dia++) {
            crearValoracion(familia, cuidador, LocalDate.of(2026, 11, dia));
        }

        List<ValoracionEntity> resultado = valoracionRepository.findTop5ByOrderByFechaDescIdDesc();

        assertThat(resultado.size(), is(equalTo(5)));
        assertThat(resultado.get(0).getFecha(), is(equalTo(LocalDate.of(2026, 11, 6))));
        assertThat(resultado.get(4).getFecha(), is(equalTo(LocalDate.of(2026, 11, 2))));
    }

    @Test
    void testConLaMismaFechaSaleAntesLaDeIdMayor() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        crearValoracion(familia, cuidador, fecha);
        ValoracionEntity ultima = crearValoracion(familia, cuidador, fecha);

        List<ValoracionEntity> resultado = valoracionRepository.findTop5ByOrderByFechaDescIdDesc();

        assertThat(resultado.get(0).getId(), is(equalTo(ultima.getId())));
    }
}