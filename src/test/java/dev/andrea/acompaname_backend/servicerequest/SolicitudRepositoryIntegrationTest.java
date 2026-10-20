package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba las consultas de SolicitudRepository contra un MySQL real (Testcontainers),
// porque un test con mocks no puede comprobar un filtro que hace la base de datos.
@DataJpaTest
@Testcontainers
public class SolicitudRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private SolicitudRepository repository;
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

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador) {
        return crearSolicitud(familia, cuidador, LocalDate.of(2026, 11, 10), null, EstadoSolicitud.PENDIENTE);
    }

    // Solicitud con las fechas y el estado que yo elija (la fecha de fin puede ser null)
    private SolicitudEntity crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador,
            LocalDate inicio, LocalDate fin, EstadoSolicitud estado) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(estado)
                .fechaCuidado(inicio)
                .fechaFin(fin)
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return repository.saveAndFlush(solicitud);
    }

    @Test
    void testLaFamiliaVeSusSolicitudes() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("ana@test.com");

        assertThat(solicitudes.size(), is(equalTo(1)));
        assertThat(solicitudes.get(0).getId(), is(equalTo(solicitud.getId())));
    }

    @Test
    void testElCuidadorVeSusSolicitudes() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("pepe@test.com");

        assertThat(solicitudes.size(), is(equalTo(1)));
        assertThat(solicitudes.get(0).getId(), is(equalTo(solicitud.getId())));
    }

    @Test
    void testUnUsuarioAjenoNoVeNingunaSolicitud() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        crearUsuario("Intruso", "intruso@test.com");
        crearSolicitud(ana, crearCuidador(pepe));

        List<SolicitudEntity> solicitudes = repository.findByParticipante("intruso@test.com");

        assertThat(solicitudes.size(), is(equalTo(0)));
    }

    @Test
    void testCadaUsuarioSoloVeSusSolicitudesAunqueHayaOtras() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        SolicitudEntity deAnaYPepe = crearSolicitud(ana, crearCuidador(pepe));
        SolicitudEntity deLuisYMarta = crearSolicitud(luis, crearCuidador(marta));

        List<SolicitudEntity> deAna = repository.findByParticipante("ana@test.com");
        List<SolicitudEntity> deMarta = repository.findByParticipante("marta@test.com");

        assertThat(deAna.size(), is(equalTo(1)));
        assertThat(deAna.get(0).getId(), is(equalTo(deAnaYPepe.getId())));
        assertThat(deMarta.size(), is(equalTo(1)));
        assertThat(deMarta.get(0).getId(), is(equalTo(deLuisYMarta.getId())));
    }

    @Test
    void testUnCuidadorConServicioAceptadoEnElRangoEstaOcupado() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);

        List<Long> ocupados = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 11), EstadoSolicitud.ACEPTADA);

        assertThat(ocupados, is(equalTo(List.of(pepe.getId()))));
    }

    @Test
    void testUnServicioSinFechaFinOcupaSoloEseDia() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 10), null, EstadoSolicitud.ACEPTADA);

        List<Long> elMismoDia = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 10), EstadoSolicitud.ACEPTADA);
        List<Long> alDiaSiguiente = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 11), EstadoSolicitud.ACEPTADA);

        assertThat(elMismoDia, is(equalTo(List.of(pepe.getId()))));
        assertThat(alDiaSiguiente, is(empty()));
    }

    @Test
    void testUnServicioEnOtrasFechasNoOcupaAlCuidador() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), EstadoSolicitud.ACEPTADA);

        List<Long> ocupados = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testSoloLasSolicitudesAceptadasOcupanAlCuidador() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        LocalDate dia = LocalDate.of(2026, 9, 10);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.RECHAZADA);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.COMPLETADA);

        List<Long> ocupados = repository.findCuidadoresOcupados(dia, dia, EstadoSolicitud.ACEPTADA);

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testUnCuidadorConDosServiciosCruzadosApareceUnaSolaVez() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 10), null, EstadoSolicitud.ACEPTADA);
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 11), null, EstadoSolicitud.ACEPTADA);

        List<Long> ocupados = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 11), EstadoSolicitud.ACEPTADA);

        assertThat(ocupados, is(equalTo(List.of(pepe.getId()))));
    }

    @Test
    void testLosDiasDeInicioYFinDelServicioCuentanComoOcupados() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 10), EstadoSolicitud.ACEPTADA);

        // El servicio acaba el 10: buscar desde el 10 sigue contando como ocupado, desde el 11 ya no
        List<Long> terminaElDiaBuscado = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);
        List<Long> terminaAntes = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);
        // El servicio empieza el 5: buscar hasta el 5 cuenta como ocupado, hasta el 4 ya no
        List<Long> empiezaElDiaBuscado = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5), EstadoSolicitud.ACEPTADA);
        List<Long> empiezaDespues = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4), EstadoSolicitud.ACEPTADA);

        assertThat(terminaElDiaBuscado, is(equalTo(List.of(pepe.getId()))));
        assertThat(terminaAntes, is(empty()));
        assertThat(empiezaElDiaBuscado, is(equalTo(List.of(pepe.getId()))));
        assertThat(empiezaDespues, is(empty()));
    }

    @Test
    void testSoloDevuelveLosCuidadoresOcupadosYNoLosLibres() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        PerfilCuidadorEntity marta = crearCuidador(crearUsuario("Marta", "marta@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 10), null, EstadoSolicitud.ACEPTADA);
        crearSolicitud(ana, marta, LocalDate.of(2026, 9, 20), null, EstadoSolicitud.ACEPTADA);

        List<Long> ocupados = repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 11), EstadoSolicitud.ACEPTADA);

        assertThat(ocupados, is(equalTo(List.of(pepe.getId()))));
    }

    @Test
    void testOtroServicioAceptadoEnElMismoDiaSeCruza() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        LocalDate dia = LocalDate.of(2026, 9, 10);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.ACEPTADA);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);

        long solapados = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(solapados, is(equalTo(1L)));
    }

    @Test
    void testUnServicioAceptadoQueCubreTodoElPeriodoSeCruza() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);
        LocalDate dia = LocalDate.of(2026, 9, 10);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);

        long solapados = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(solapados, is(equalTo(1L)));
    }

    @Test
    void testElUltimoDiaDelServicioAceptadoSeCruzaPeroElSiguienteNo() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 10), EstadoSolicitud.ACEPTADA);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12), EstadoSolicitud.PENDIENTE);

        long empiezaElDiaEnQueTermina = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);
        long empiezaAlDiaSiguiente = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(),
                LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12), EstadoSolicitud.ACEPTADA);

        assertThat(empiezaElDiaEnQueTermina, is(equalTo(1L)));
        assertThat(empiezaAlDiaSiguiente, is(equalTo(0L)));
    }

    @Test
    void testUnServicioAceptadoEnOtrasFechasNoSeCruza() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(ana, pepe, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 15), EstadoSolicitud.ACEPTADA);
        LocalDate dia = LocalDate.of(2026, 9, 10);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);

        long solapados = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(solapados, is(equalTo(0L)));
    }

    @Test
    void testSoloLosServiciosAceptadosBloquean() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        LocalDate dia = LocalDate.of(2026, 9, 10);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.RECHAZADA);
        crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.COMPLETADA);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);

        long solapados = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(solapados, is(equalTo(0L)));
    }

    @Test
    void testUnServicioAceptadoDeOtroCuidadorNoBloquea() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        PerfilCuidadorEntity marta = crearCuidador(crearUsuario("Marta", "marta@test.com"));
        LocalDate dia = LocalDate.of(2026, 9, 10);
        crearSolicitud(ana, marta, dia, null, EstadoSolicitud.ACEPTADA);
        SolicitudEntity laQueSeAcepta = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.PENDIENTE);

        long solapados = repository.countSolapamientos(pepe.getId(), laQueSeAcepta.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(solapados, is(equalTo(0L)));
    }

    @Test
    void testLaPropiaSolicitudNoCuentaComoSolapamiento() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity pepe = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        LocalDate dia = LocalDate.of(2026, 9, 10);
        SolicitudEntity aceptada = crearSolicitud(ana, pepe, dia, null, EstadoSolicitud.ACEPTADA);

        long contandoLaPropia = repository.countSolapamientos(pepe.getId(), aceptada.getId() + 1, dia, dia,
                EstadoSolicitud.ACEPTADA);
        long excluyendoLaPropia = repository.countSolapamientos(pepe.getId(), aceptada.getId(), dia, dia,
                EstadoSolicitud.ACEPTADA);

        assertThat(contandoLaPropia, is(equalTo(1L)));
        assertThat(excluyendoLaPropia, is(equalTo(0L)));
    }
}