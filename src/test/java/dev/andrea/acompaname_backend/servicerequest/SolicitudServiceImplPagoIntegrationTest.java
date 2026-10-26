package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba con el servicio de verdad y un MySQL real (Testcontainers), sin mocks,
// que el pago de una solicitud se genera al completarla y no antes.
@DataJpaTest
@Testcontainers
@Import(SolicitudServiceImpl.class)
public class SolicitudServiceImplPagoIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private SolicitudServiceImpl service;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

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

    // Solicitud pendiente de Ana (familia) a Pepe (cuidador), y entra Pepe en la aplicación
    private SolicitudEntity crearSolicitudYEntrarComoCuidador() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .familia(ana)
                .cuidador(crearCuidador(pepe))
                .build();
        solicitud = solicitudRepository.saveAndFlush(solicitud);
        loguearComo("pepe@test.com");
        return solicitud;
    }

    @Test
    void testAceptarLaSolicitudNoCreaPago() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);

        assertThat(pagoRepository.existsBySolicitudId(solicitud.getId()), is(equalTo(false)));
    }

    @Test
    void testCompletarLaSolicitudCreaSuPagoPendienteConLaTarifaDelCuidador() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);

        List<PagoEntity> pagos = pagoRepository.findAll();
        assertThat(pagos.size(), is(equalTo(1)));
        assertThat(pagos.get(0).getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pagos.get(0).getImporte().compareTo(new BigDecimal("18.00")), is(equalTo(0)));
    }

    @Test
    void testRechazarLaSolicitudNoCreaPago() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.RECHAZADA);

        assertThat(pagoRepository.existsBySolicitudId(solicitud.getId()), is(equalTo(false)));
    }

    @Test
    void testCompletarDosVecesLaSolicitudNoDuplicaElPago() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);

        assertThat(pagoRepository.count(), is(equalTo(1L)));
    }
}