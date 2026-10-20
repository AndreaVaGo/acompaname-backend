package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real (Testcontainers) las consultas de los avisos del administrador
// y la que cuenta los servicios de un cuidador: el filtro por fechas y estados lo hace la base de datos.
@DataJpaTest
@Testcontainers
public class AdminAvisosIntegrationTest {

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
    private PagoRepository pagoRepository;

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
            EstadoSolicitud estado, LocalDate fechaCuidado) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(estado)
                .fechaCuidado(fechaCuidado)
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return solicitudRepository.saveAndFlush(solicitud);
    }

    // Cada pago es de una solicitud distinta (una solicitud solo puede tener un pago)
    private void crearPago(UsuarioEntity familia, PerfilCuidadorEntity cuidador, EstadoPago estado,
            LocalDate fecha) {
        SolicitudEntity solicitud = crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA,
                LocalDate.of(2026, 11, 10));
        PagoEntity pago = PagoEntity.builder()
                .importe(new BigDecimal("36.00"))
                .estado(estado)
                .fecha(fecha)
                .solicitud(solicitud)
                .build();
        pagoRepository.saveAndFlush(pago);
    }

    @Test
    void testCuentaLasPendientesQueSonEnLosProximosDias() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, hoy);
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, hoy.plusDays(2));
        // Esta queda fuera por fecha (dentro de 3 días) y esta otra por estado (ya está aceptada)
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, hoy.plusDays(3));
        crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA, hoy);

        long proximas = solicitudRepository.countByEstadoAndFechaCuidadoBetween(EstadoSolicitud.PENDIENTE, hoy,
                hoy.plusDays(2));

        assertThat(proximas, is(equalTo(2L)));
    }

    @Test
    void testCuentaLasPendientesCuyaFechaYaHaPasado() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, hoy.minusDays(1));
        // Hoy todavía no ha pasado, y una aceptada con fecha pasada no es un aviso
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, hoy);
        crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA, hoy.minusDays(5));

        long vencidas = solicitudRepository.countByEstadoAndFechaCuidadoBefore(EstadoSolicitud.PENDIENTE, hoy);

        assertThat(vencidas, is(equalTo(1L)));
    }

    @Test
    void testCuentaLosServiciosDeUnCuidadorEnUnEstado() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        PerfilCuidadorEntity marta = crearCuidador(crearUsuario("Marta", "marta@test.com"));
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        crearSolicitud(familia, pepe, EstadoSolicitud.COMPLETADA, fecha);
        crearSolicitud(familia, pepe, EstadoSolicitud.COMPLETADA, fecha);
        crearSolicitud(familia, pepe, EstadoSolicitud.PENDIENTE, fecha);
        crearSolicitud(familia, marta, EstadoSolicitud.COMPLETADA, fecha);

        long completadosDePepe = solicitudRepository.countByCuidadorIdAndEstado(pepe.getId(),
                EstadoSolicitud.COMPLETADA);

        assertThat(completadosDePepe, is(equalTo(2L)));
    }

    @Test
    void testCuentaLosPagosPendientesDeHaceMasDeUnaSemana() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearPago(familia, cuidador, EstadoPago.PENDIENTE, hoy.minusDays(10));
        // Este es reciente y este otro ya está cobrado: no son avisos
        crearPago(familia, cuidador, EstadoPago.PENDIENTE, hoy.minusDays(3));
        crearPago(familia, cuidador, EstadoPago.COMPLETADO, hoy.minusDays(10));

        long sinCobrar = pagoRepository.countByEstadoAndFechaBefore(EstadoPago.PENDIENTE, hoy.minusDays(7));

        assertThat(sinCobrar, is(equalTo(1L)));
    }
}