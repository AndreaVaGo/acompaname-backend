package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;

// Prueba con el servicio de verdad y un MySQL real (Testcontainers), sin mocks,
// que el pago de una solicitud se genera al completarla y no antes.
@DataJpaTest
@Import(SolicitudServiceImpl.class)
public class SolicitudServiceImplPagoIntegrationTest extends ContenedorMySqlRepositorios {

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
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(14, 0))
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
    void testCompletarLaSolicitudCreaSuPagoPendienteConHorasPorTarifa() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);

        List<PagoEntity> pagos = pagoRepository.findAll();
        assertThat(pagos.size(), is(equalTo(1)));
        assertThat(pagos.get(0).getEstado(), is(equalTo(EstadoPago.PENDIENTE)));
        // 4 horas (de 10:00 a 14:00) x 18,00 = 72,00
        assertThat(pagos.get(0).getImporte().compareTo(new BigDecimal("72.00")), is(equalTo(0)));
    }

    @Test
    void testCadaPersonaExtraSumaUnRecargoDelVeinticincoPorCiento() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));
        solicitudRepository.saveAndFlush(solicitud);
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);

        // 4 horas x 18,00 x (1 + 0,25 x 1 persona extra) = 90,00
        List<PagoEntity> pagos = pagoRepository.findAll();
        assertThat(pagos.get(0).getImporte().compareTo(new BigDecimal("90.00")), is(equalTo(0)));
    }

    @Test
    void testRechazarLaSolicitudNoCreaPago() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();

        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.RECHAZADA);

        assertThat(pagoRepository.existsBySolicitudId(solicitud.getId()), is(equalTo(false)));
    }

    @Test
    void testNoSePuedeBorrarUnaSolicitudCompletadaYSuPagoSigueAhi() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);
        loguearComo("ana@test.com");

        assertThrows(SolicitudExceptionEstadoInvalido.class, () -> service.deleteById(solicitud.getId()));

        assertThat(solicitudRepository.existsById(solicitud.getId()), is(equalTo(true)));
        assertThat(pagoRepository.count(), is(equalTo(1L)));
    }

    @Test
    void testSePuedeBorrarUnaSolicitudPendiente() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        loguearComo("ana@test.com");

        service.deleteById(solicitud.getId());

        assertThat(solicitudRepository.existsById(solicitud.getId()), is(equalTo(false)));
    }

    @Test
    void testCompletarDosVecesLaSolicitudDaErrorYNoDuplicaElPago() {
        SolicitudEntity solicitud = crearSolicitudYEntrarComoCuidador();
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.ACEPTADA);
        service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA);

        assertThrows(SolicitudExceptionEstadoInvalido.class,
                () -> service.cambiarEstado(solicitud.getId(), EstadoSolicitud.COMPLETADA));

        assertThat(pagoRepository.count(), is(equalTo(1L)));
    }
}