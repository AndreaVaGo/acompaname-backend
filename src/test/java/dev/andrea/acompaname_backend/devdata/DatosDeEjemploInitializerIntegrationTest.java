package dev.andrea.acompaname_backend.devdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoEntity;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real que los datos de ejemplo de desarrollo se crean bien,
// no se duplican al ejecutarlos otra vez y solo existen con el perfil "dev".
// Cada test se deshace al terminar (rollback), así que no deja datos en la base compartida.
@DataJpaTest
@ActiveProfiles("dev")
@Import(DatosDeEjemploInitializer.class)
public class DatosDeEjemploInitializerIntegrationTest extends ContenedorMySqlRepositorios {

    @TestConfiguration
    static class Configuracion {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private DatosDeEjemploInitializer inicializador;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // La aplicación crea estos roles al arrancar (RoleInitializer); en el test los creamos a mano
    private void crearRoles() {
        String[] nombres = { "FAMILIA", "CUIDADOR", "ADMIN" };
        for (String nombre : nombres) {
            RoleEntity rol = new RoleEntity();
            rol.setName(nombre);
            roleRepository.saveAndFlush(rol);
        }
    }

    private long solicitudesEn(EstadoSolicitud estado) {
        long total = 0;
        for (SolicitudEntity solicitud : solicitudRepository.findAll()) {
            if (solicitud.getEstado() == estado) {
                total++;
            }
        }
        return total;
    }

    @Test
    void testCreaLaFamiliaYLasDosPersonasCuidadorasConSuRol() throws Exception {
        crearRoles();

        inicializador.run();

        assertThat(usuarioRepository.count(), is(equalTo(3L)));
        UsuarioEntity familia = usuarioRepository.findByEmail("familia@test.com").orElseThrow();
        assertThat(familia.getRoles().iterator().next().getName(), is(equalTo("FAMILIA")));
        UsuarioEntity cuidador = usuarioRepository.findByEmail("cuidador@test.com").orElseThrow();
        assertThat(cuidador.getRoles().iterator().next().getName(), is(equalTo("CUIDADOR")));
        assertThat(usuarioRepository.findByEmail("cuidadora@test.com").isPresent(), is(true));
    }

    @Test
    void testLaContrasenaSeGuardaCifradaYSirveParaEntrar() throws Exception {
        crearRoles();

        inicializador.run();

        UsuarioEntity familia = usuarioRepository.findByEmail("familia@test.com").orElseThrow();
        assertThat(familia.getPassword().equals("Password123!"), is(false));
        assertThat(passwordEncoder.matches("Password123!", familia.getPassword()), is(true));
    }

    @Test
    void testLasPersonasCuidadorasTienenPerfil() throws Exception {
        crearRoles();

        inicializador.run();

        assertThat(perfilRepository.count(), is(equalTo(2L)));
    }

    @Test
    void testCreaUnaSolicitudDeCadaEstadoConHoras() throws Exception {
        crearRoles();

        inicializador.run();

        assertThat(solicitudRepository.count(), is(equalTo(4L)));
        assertThat(solicitudesEn(EstadoSolicitud.PENDIENTE), is(equalTo(1L)));
        assertThat(solicitudesEn(EstadoSolicitud.ACEPTADA), is(equalTo(1L)));
        assertThat(solicitudesEn(EstadoSolicitud.COMPLETADA), is(equalTo(1L)));
        assertThat(solicitudesEn(EstadoSolicitud.RECHAZADA), is(equalTo(1L)));
        for (SolicitudEntity solicitud : solicitudRepository.findAll()) {
            assertThat(solicitud.getHoraInicio() != null, is(true));
            assertThat(solicitud.getHoraFin() != null, is(true));
        }
    }

    @Test
    void testLaSolicitudPendienteEsDeDosPersonasYLasDemasDeUna() throws Exception {
        crearRoles();

        inicializador.run();

        for (SolicitudEntity solicitud : solicitudRepository.findAll()) {
            if (solicitud.getEstado() == EstadoSolicitud.PENDIENTE) {
                assertThat(solicitud.getPersonas().size(), is(equalTo(2)));
                assertThat(solicitud.getPersonas().get(0).getNombre(), is(equalTo("Carmen")));
                assertThat(solicitud.getPersonas().get(1).getNombre(), is(equalTo("Antonio")));
            } else {
                assertThat(solicitud.getPersonas().size(), is(equalTo(1)));
            }
        }
    }

    @Test
    void testCreaUnPagoPendienteYUnoCompletado() throws Exception {
        crearRoles();

        inicializador.run();

        assertThat(pagoRepository.count(), is(equalTo(2L)));
        int pendientes = 0;
        int completados = 0;
        for (PagoEntity pago : pagoRepository.findAll()) {
            if (pago.getEstado() == EstadoPago.PENDIENTE) {
                pendientes++;
            }
            if (pago.getEstado() == EstadoPago.COMPLETADO) {
                completados++;
            }
        }
        assertThat(pendientes, is(equalTo(1)));
        assertThat(completados, is(equalTo(1)));
    }

    @Test
    void testSiYaExistenNoSeDuplican() throws Exception {
        crearRoles();

        inicializador.run();
        inicializador.run();

        assertThat(usuarioRepository.count(), is(equalTo(3L)));
        assertThat(solicitudRepository.count(), is(equalTo(4L)));
        assertThat(pagoRepository.count(), is(equalTo(2L)));
    }

    @Test
    void testSinLosRolesNoCreaNada() throws Exception {
        inicializador.run();

        assertThat(usuarioRepository.count(), is(equalTo(0L)));
        assertThat(solicitudRepository.count(), is(equalTo(0L)));
    }

    @Test
    void testSoloSeActivaConElPerfilDev() {
        Profile perfil = DatosDeEjemploInitializer.class.getAnnotation(Profile.class);

        assertThat(perfil.value(), is(arrayContaining("dev")));
    }
}
