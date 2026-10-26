package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
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

// Prueba contra un MySQL real (Testcontainers) las consultas de la lista de pagos del administrador
// y el servicio que calcula el resumen con esos datos.
@DataJpaTest
@Testcontainers
public class AdminPagosIntegrationTest {

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

    // Cada pago es de una solicitud distinta (una solicitud solo puede tener un pago)
    private PagoEntity crearPago(UsuarioEntity familia, PerfilCuidadorEntity cuidador, String tipoCuidado,
            String importe, EstadoPago estado, LocalDate fecha) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado(tipoCuidado)
                .estado(EstadoSolicitud.COMPLETADA)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        solicitud = solicitudRepository.saveAndFlush(solicitud);
        PagoEntity pago = PagoEntity.builder()
                .importe(new BigDecimal(importe))
                .estado(estado)
                .fecha(fecha)
                .solicitud(solicitud)
                .build();
        return pagoRepository.saveAndFlush(pago);
    }

    @Test
    void testLaListaSaleDelPagoMasRecienteAlMasAntiguo() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        PagoEntity antiguo = crearPago(familia, cuidador, "domicilio", "10.00", EstadoPago.PENDIENTE, hoy.minusDays(10));
        PagoEntity reciente = crearPago(familia, cuidador, "domicilio", "20.00", EstadoPago.PENDIENTE, hoy.minusDays(1));
        PagoEntity medio = crearPago(familia, cuidador, "domicilio", "30.00", EstadoPago.PENDIENTE, hoy.minusDays(5));
        // Con la misma fecha que el anterior, sale primero el que se creó después
        PagoEntity medioPosterior = crearPago(familia, cuidador, "domicilio", "40.00", EstadoPago.PENDIENTE,
                hoy.minusDays(5));

        List<PagoEntity> resultado = pagoRepository.findAllByOrderByFechaDescIdDesc();

        assertThat(resultado, hasSize(4));
        assertThat(resultado.get(0).getId(), is(equalTo(reciente.getId())));
        assertThat(resultado.get(1).getId(), is(equalTo(medioPosterior.getId())));
        assertThat(resultado.get(2).getId(), is(equalTo(medio.getId())));
        assertThat(resultado.get(3).getId(), is(equalTo(antiguo.getId())));
    }

    @Test
    void testLaListaPuedeFiltrarPorEstado() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearPago(familia, cuidador, "domicilio", "10.00", EstadoPago.PENDIENTE, hoy);
        crearPago(familia, cuidador, "domicilio", "20.00", EstadoPago.COMPLETADO, hoy);
        crearPago(familia, cuidador, "domicilio", "30.00", EstadoPago.COMPLETADO, hoy.minusDays(2));

        List<PagoEntity> completados = pagoRepository.findByEstadoOrderByFechaDescIdDesc(EstadoPago.COMPLETADO);

        assertThat(completados, hasSize(2));
        assertThat(completados.get(0).getImporte(), is(comparesEqualTo(new BigDecimal("20.00"))));
        assertThat(completados.get(1).getImporte(), is(comparesEqualTo(new BigDecimal("30.00"))));
    }

    @Test
    void testElServicioCalculaElResumenConDatosReales() {
        LocalDate hoy = LocalDate.now();
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        PerfilCuidadorEntity marta = crearCuidador(crearUsuario("Marta", "marta@test.com"));
        crearPago(familia, pepe, "domicilio", "36.00", EstadoPago.COMPLETADO, hoy);
        crearPago(familia, pepe, "domicilio", "18.00", EstadoPago.PENDIENTE, hoy.minusDays(10));
        crearPago(familia, marta, "hospitalario", "20.00", EstadoPago.COMPLETADO, hoy);
        crearPago(familia, marta, "hospitalario", "99.00", EstadoPago.CANCELADO, hoy);
        AdminPagosServiceImpl service = new AdminPagosServiceImpl(pagoRepository);

        AdminPagosResumenDTOResponse resultado = service.resumen();

        assertThat(resultado.totalCobrado(), is(comparesEqualTo(new BigDecimal("56.00"))));
        assertThat(resultado.totalPendiente(), is(comparesEqualTo(new BigDecimal("18.00"))));
        assertThat(resultado.numeroCobrados(), is(equalTo(2L)));
        assertThat(resultado.numeroPendientes(), is(equalTo(1L)));
        assertThat(resultado.numeroCancelados(), is(equalTo(1L)));
        assertThat(resultado.pendientesMasDeSieteDias(), is(equalTo(1L)));
        assertThat(resultado.porcentajeCobrado(), is(equalTo(66.7)));
        assertThat(resultado.porTipoCuidado().get(0).importe(), is(comparesEqualTo(new BigDecimal("20.00"))));
        assertThat(resultado.porTipoCuidado().get(1).importe(), is(comparesEqualTo(new BigDecimal("54.00"))));
        assertThat(resultado.topCuidadores().get(0).nombre(), is(equalTo("Pepe")));
        assertThat(resultado.topCuidadores().get(1).nombre(), is(equalTo("Marta")));
    }

    @Test
    void testElServicioListaLosPagosConLosNombres() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearPago(familia, cuidador, "hospitalario", "36.00", EstadoPago.PENDIENTE, LocalDate.now().minusDays(2));
        AdminPagosServiceImpl service = new AdminPagosServiceImpl(pagoRepository);

        List<AdminPagoDTOResponse> resultado = service.pagos(null);

        assertThat(resultado, hasSize(1));
        assertThat(resultado.get(0).nombreFamilia(), is(equalTo("Ana")));
        assertThat(resultado.get(0).nombreCuidador(), is(equalTo("Pepe")));
        assertThat(resultado.get(0).tipoCuidado(), is(equalTo("hospitalario")));
        assertThat(resultado.get(0).diasDesdeCreacion(), is(equalTo(2L)));
    }
}