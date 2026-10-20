package dev.andrea.acompaname_backend.payment;

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
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba la consulta findByParticipante contra un MySQL real (Testcontainers),
// porque un test con mocks no puede comprobar un filtro que hace la base de datos.
@DataJpaTest
@Testcontainers
public class PagoRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private PagoRepository repository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

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

    // Crea una solicitud de la familia al cuidador y su pago
    private PagoEntity crearPago(UsuarioEntity familia, PerfilCuidadorEntity cuidador) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.ACEPTADA)
                .familia(familia)
                .cuidador(cuidador)
                .build();
        solicitudRepository.saveAndFlush(solicitud);

        PagoEntity pago = PagoEntity.builder()
                .importe(new BigDecimal("90.00"))
                .estado(EstadoPago.PENDIENTE)
                .fecha(LocalDate.now())
                .solicitud(solicitud)
                .build();
        return repository.saveAndFlush(pago);
    }

    @Test
    void testLaFamiliaVeElPagoDeSuSolicitud() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        PagoEntity pago = crearPago(ana, crearCuidador(pepe));

        List<PagoEntity> pagos = repository.findByParticipante("ana@test.com");

        assertThat(pagos.size(), is(equalTo(1)));
        assertThat(pagos.get(0).getId(), is(equalTo(pago.getId())));
    }

    @Test
    void testElCuidadorVeElPagoDeSuSolicitud() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        PagoEntity pago = crearPago(ana, crearCuidador(pepe));

        List<PagoEntity> pagos = repository.findByParticipante("pepe@test.com");

        assertThat(pagos.size(), is(equalTo(1)));
        assertThat(pagos.get(0).getId(), is(equalTo(pago.getId())));
    }

    @Test
    void testUnUsuarioAjenoNoVeNingunPago() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        crearUsuario("Intruso", "intruso@test.com");
        crearPago(ana, crearCuidador(pepe));

        List<PagoEntity> pagos = repository.findByParticipante("intruso@test.com");

        assertThat(pagos.size(), is(equalTo(0)));
    }

    @Test
    void testCadaUsuarioSoloVeSusPagosAunqueHayaOtros() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        PagoEntity pagoAnaPepe = crearPago(ana, crearCuidador(pepe));
        PagoEntity pagoLuisMarta = crearPago(luis, crearCuidador(marta));

        List<PagoEntity> deAna = repository.findByParticipante("ana@test.com");
        List<PagoEntity> deMarta = repository.findByParticipante("marta@test.com");

        assertThat(deAna.size(), is(equalTo(1)));
        assertThat(deAna.get(0).getId(), is(equalTo(pagoAnaPepe.getId())));
        assertThat(deMarta.size(), is(equalTo(1)));
        assertThat(deMarta.get(0).getId(), is(equalTo(pagoLuisMarta.getId())));
    }
}