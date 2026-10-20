package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
import dev.andrea.acompaname_backend.review.ValoracionEntity;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real (Testcontainers) las consultas que usa el panel del administrador:
// los conteos, las sumas y las medias las hace la base de datos, y un mock no puede comprobarlas.
@DataJpaTest
@Testcontainers
public class AdminConsultasIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private ValoracionRepository valoracionRepository;

    private RoleEntity crearRol(String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setName(nombre);
        return roleRepository.saveAndFlush(rol);
    }

    private UsuarioEntity crearUsuario(String nombre, String email, RoleEntity rol) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .roles(Set.of(rol))
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    private PerfilCuidadorEntity crearCuidador(UsuarioEntity usuario, boolean disponible) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .disponibleAhora(disponible)
                .usuario(usuario)
                .build();
        return perfilCuidadorRepository.saveAndFlush(perfil);
    }

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador,
            EstadoSolicitud estado, String tipoCuidado) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado(tipoCuidado)
                .estado(estado)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return solicitudRepository.saveAndFlush(solicitud);
    }

    // Crea una familia y un cuidador para usarlos en las solicitudes de cada test
    private UsuarioEntity crearFamilia() {
        return crearUsuario("Ana", "ana@test.com", crearRol("FAMILIA"));
    }

    private PerfilCuidadorEntity crearCuidadorPepe() {
        return crearCuidador(crearUsuario("Pepe", "pepe@test.com", crearRol("CUIDADOR")), true);
    }

    @Test
    void testCuentaLosUsuariosDeCadaRol() {
        RoleEntity familia = crearRol("FAMILIA");
        RoleEntity cuidador = crearRol("CUIDADOR");
        crearUsuario("Ana", "ana@test.com", familia);
        crearUsuario("Luis", "luis@test.com", familia);
        crearUsuario("Pepe", "pepe@test.com", cuidador);

        assertThat(usuarioRepository.countByRolesName("FAMILIA"), is(equalTo(2L)));
        assertThat(usuarioRepository.countByRolesName("CUIDADOR"), is(equalTo(1L)));
        assertThat(usuarioRepository.countByRolesName("ADMIN"), is(equalTo(0L)));
    }

    @Test
    void testCuentaLosCuidadoresDisponiblesAhora() {
        RoleEntity rol = crearRol("CUIDADOR");
        crearCuidador(crearUsuario("Pepe", "pepe@test.com", rol), true);
        crearCuidador(crearUsuario("Marta", "marta@test.com", rol), false);

        assertThat(perfilCuidadorRepository.countByDisponibleAhoraTrue(), is(equalTo(1L)));
    }

    @Test
    void testCuentaLasSolicitudesPorEstado() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidadorPepe();
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "domicilio");
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "domicilio");
        crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA, "domicilio");

        assertThat(solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE), is(equalTo(2L)));
        assertThat(solicitudRepository.countByEstado(EstadoSolicitud.ACEPTADA), is(equalTo(1L)));
        assertThat(solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA), is(equalTo(0L)));
    }

    @Test
    void testCuentaLasSolicitudesPorTipoDeCuidado() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidadorPepe();
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "hospitalario");
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "domicilio");
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "domicilio");

        assertThat(solicitudRepository.countByTipoCuidado("hospitalario"), is(equalTo(1L)));
        assertThat(solicitudRepository.countByTipoCuidado("domicilio"), is(equalTo(2L)));
    }

    @Test
    void testSoloDevuelveLasSolicitudesConFechaDeAceptacion() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidadorPepe();
        SolicitudEntity aceptada = crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA, "domicilio");
        aceptada.setFechaAceptacion(LocalDateTime.of(2026, 11, 10, 12, 0));
        solicitudRepository.saveAndFlush(aceptada);
        crearSolicitud(familia, cuidador, EstadoSolicitud.PENDIENTE, "domicilio");

        List<SolicitudEntity> resultado = solicitudRepository.findByFechaAceptacionIsNotNull();

        assertThat(resultado.size(), is(equalTo(1)));
        assertThat(resultado.get(0).getId(), is(equalTo(aceptada.getId())));
    }

    // Cada pago es de una solicitud distinta (una solicitud solo puede tener un pago)
    private void crearPago(UsuarioEntity familia, PerfilCuidadorEntity cuidador, String importe,
            EstadoPago estado) {
        SolicitudEntity solicitud = crearSolicitud(familia, cuidador, EstadoSolicitud.ACEPTADA, "domicilio");
        PagoEntity pago = PagoEntity.builder()
                .importe(new BigDecimal(importe))
                .estado(estado)
                .fecha(LocalDate.of(2026, 11, 10))
                .solicitud(solicitud)
                .build();
        pagoRepository.saveAndFlush(pago);
    }

    @Test
    void testSumaLosPagosDeUnEstado() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidadorPepe();
        crearPago(familia, cuidador, "10.00", EstadoPago.COMPLETADO);
        crearPago(familia, cuidador, "20.50", EstadoPago.COMPLETADO);
        crearPago(familia, cuidador, "36.00", EstadoPago.PENDIENTE);

        BigDecimal cobrados = pagoRepository.sumaPorEstado(EstadoPago.COMPLETADO);
        BigDecimal pendientes = pagoRepository.sumaPorEstado(EstadoPago.PENDIENTE);

        assertThat(cobrados.compareTo(new BigDecimal("30.50")), is(equalTo(0)));
        assertThat(pendientes.compareTo(new BigDecimal("36.00")), is(equalTo(0)));
    }

    @Test
    void testLaSumaDePagosSinPagosEsNull() {
        assertThat(pagoRepository.sumaPorEstado(EstadoPago.CANCELADO), is(nullValue()));
    }

    // Cada valoración es de una solicitud distinta (una solicitud solo puede tener una valoración)
    private void crearValoracion(UsuarioEntity familia, PerfilCuidadorEntity cuidador, int puntuacion) {
        SolicitudEntity solicitud = crearSolicitud(familia, cuidador, EstadoSolicitud.COMPLETADA, "domicilio");
        ValoracionEntity valoracion = ValoracionEntity.builder()
                .comentario("Muy bien")
                .puntuacion(puntuacion)
                .fecha(LocalDate.of(2026, 11, 10))
                .solicitud(solicitud)
                .build();
        valoracionRepository.saveAndFlush(valoracion);
    }

    @Test
    void testLaMediaGeneralDeLasValoraciones() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidadorPepe();
        crearValoracion(familia, cuidador, 4);
        crearValoracion(familia, cuidador, 5);

        assertThat(valoracionRepository.mediaGeneral(), is(equalTo(4.5)));
    }

    @Test
    void testLaMediaGeneralSinValoracionesEsNull() {
        assertThat(valoracionRepository.mediaGeneral(), is(nullValue()));
    }
}