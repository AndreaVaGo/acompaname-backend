package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba el pago con el servicio de verdad y un MySQL real (Testcontainers), sin mocks.
// El pago no se crea desde el servicio de pagos: lo crea el servicio de solicitudes al completarla
// (se prueba en SolicitudServiceImplPagoIntegrationTest). Aquí se comprueba que pagar no cambia el importe.
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

    // Guarda un pago pendiente de 18,00 € de una solicitud completada de Ana (familia) a Pepe (cuidador)
    private PagoEntity crearPagoPendiente() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = solicitudRepository.saveAndFlush(SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.COMPLETADA)
                .familia(ana)
                .cuidador(crearCuidador(pepe))
                .build());
        return pagoRepository.saveAndFlush(PagoEntity.builder()
                .importe(new BigDecimal("18.00"))
                .estado(EstadoPago.PENDIENTE)
                .fecha(LocalDate.of(2026, 11, 10))
                .solicitud(solicitud)
                .build());
    }

    @Test
    void testLaFamiliaPuedePagarUnPagoPendiente() {
        PagoEntity pago = crearPagoPendiente();
        loguearComo("ana@test.com");

        PagoDTOResponse pagado = service.marcarComoPagado(pago.getId());

        assertThat(pagado.estado(), is(equalTo(EstadoPago.COMPLETADO)));
        assertThat(pagoRepository.findById(pago.getId()).get().getEstado(), is(equalTo(EstadoPago.COMPLETADO)));
    }

    @Test
    void testPagarNoCambiaElImporte() {
        PagoEntity pago = crearPagoPendiente();
        loguearComo("ana@test.com");

        PagoDTOResponse pagado = service.marcarComoPagado(pago.getId());

        assertThat(pagado.importe().compareTo(new BigDecimal("18.00")), is(equalTo(0)));
    }

    @Test
    void testNoSePuedePagarDosVeces() {
        PagoEntity pago = crearPagoPendiente();
        loguearComo("ana@test.com");
        service.marcarComoPagado(pago.getId());

        assertThrows(PagoExceptionConflicto.class, () -> service.marcarComoPagado(pago.getId()));
    }

    @Test
    void testElCuidadorNoPuedePagarElPagoYQuedaPendiente() {
        PagoEntity pago = crearPagoPendiente();
        loguearComo("pepe@test.com");

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.marcarComoPagado(pago.getId()));

        assertThat(pagoRepository.findById(pago.getId()).get().getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
    }
}