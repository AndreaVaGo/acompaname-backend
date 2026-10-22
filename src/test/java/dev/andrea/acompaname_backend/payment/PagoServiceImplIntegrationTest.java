package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

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
import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba la regla "solo se paga una solicitud completada" con el servicio de verdad
// y un MySQL real (Testcontainers), sin mocks.
@DataJpaTest
@Testcontainers
@Import(PagoServiceImpl.class)
public class PagoServiceImplIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private PagoServiceImpl service;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
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

    // Crea la solicitud de Ana (familia) a Pepe (cuidador) con el estado que se pida
    private SolicitudEntity crearSolicitud(EstadoSolicitud estado) {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(estado)
                .familia(ana)
                .cuidador(crearCuidador(pepe))
                .build();
        return solicitudRepository.saveAndFlush(solicitud);
    }

    @Test
    void testNoSeCreaElPagoDeUnaSolicitudAceptada() {
        SolicitudEntity solicitud = crearSolicitud(EstadoSolicitud.ACEPTADA);
        loguearComo("ana@test.com");

        assertThrows(PagoExceptionConflicto.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), solicitud.getId())));

        assertThat(pagoRepository.existsBySolicitudId(solicitud.getId()), is(equalTo(false)));
    }

    @Test
    void testSeCreaElPagoDeUnaSolicitudCompletada() {
        SolicitudEntity solicitud = crearSolicitud(EstadoSolicitud.COMPLETADA);
        loguearComo("ana@test.com");

        PagoDTOResponse pago = service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), solicitud.getId()));

        assertThat(pago.estado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pagoRepository.existsBySolicitudId(solicitud.getId()), is(equalTo(true)));
    }

    @Test
    void testElPagoDeUnaSolicitudCompletadaSePuedePagar() {
        SolicitudEntity solicitud = crearSolicitud(EstadoSolicitud.COMPLETADA);
        loguearComo("ana@test.com");
        PagoDTOResponse pago = service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), solicitud.getId()));

        PagoDTOResponse pagado = service.marcarComoPagado(pago.id());

        assertThat(pagado.estado(), is(equalTo(EstadoPago.COMPLETADO)));
    }
}