package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionSolapamiento;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de cómo afectan los bloques ocupados del calendario a la búsqueda de las familias
// y a aceptar solicitudes
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplBloquesTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private BloqueOcupadoRepository bloqueRepository;

    private SolicitudServiceImpl service;
    private SecurityContext contextoOriginal;

    private static final LocalDate DIA = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository, bloqueRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private PerfilCuidadorEntity perfilPepe() {
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe);
    }

    // Bloque de Pepe (cuidador 1) de ese día, de la hora "desde" a la hora "hasta"
    private BloqueOcupadoEntity bloqueDePepe(int desde, int hasta) {
        return BloqueOcupadoEntity.builder()
                .cuidador(PerfilCuidadorEntity.builder().id(1L).build())
                .fecha(DIA)
                .horaInicio(LocalTime.of(desde, 0))
                .horaFin(LocalTime.of(hasta, 0))
                .build();
    }

    private void elCalendarioDevuelve(BloqueOcupadoEntity... bloques) {
        when(bloqueRepository.findByFechaBetween(DIA, DIA)).thenReturn(List.of(bloques));
    }

    // ---------- Búsqueda de las familias ----------

    @Test
    void testUnBloqueQueSeCruzaDejaOcupadaALaPersonaCuidadora() {
        elCalendarioDevuelve(bloqueDePepe(8, 15));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testUnBloqueEnOtrasHorasNoLaDejaOcupada() {
        elCalendarioDevuelve(bloqueDePepe(8, 15));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(16, 0), LocalTime.of(18, 0));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testUnBloqueQueAcabaJustoCuandoEmpiezaLaBusquedaNoLaDejaOcupada() {
        elCalendarioDevuelve(bloqueDePepe(8, 15));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(15, 0), LocalTime.of(18, 0));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testBuscandoElDiaCompletoUnBloqueLaDejaOcupada() {
        elCalendarioDevuelve(bloqueDePepe(8, 9));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, null, null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSiTieneUnServicioYUnBloqueQueSeCruzanSoloSalePepeUnaVez() {
        SolicitudEntity aceptada = SolicitudEntity.builder()
                .estado(EstadoSolicitud.ACEPTADA)
                .fechaCuidado(DIA)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(12, 0))
                .cuidador(PerfilCuidadorEntity.builder().id(1L).build())
                .build();
        when(repository.findPorFechasQueSeCruzan(DIA, DIA, EstadoSolicitud.ACEPTADA)).thenReturn(List.of(aceptada));
        elCalendarioDevuelve(bloqueDePepe(8, 15));

        List<Long> ocupados = service.getCuidadoresOcupados(DIA, null, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    // ---------- Aceptar una solicitud ----------

    // Solicitud pendiente de Ana a Pepe, ese día de 09:00 a 12:00
    private SolicitudEntity solicitudDeNueveADoce() {
        UsuarioEntity ana = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rol(1L, "FAMILIA")));
        SolicitudEntity solicitud = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, DIA,
                EstadoSolicitud.PENDIENTE, ana, perfilPepe());
        solicitud.setHoraInicio(LocalTime.of(9, 0));
        solicitud.setHoraFin(LocalTime.of(12, 0));
        return solicitud;
    }

    @Test
    void testAceptarConUnBloqueQueSeCruzaDaSolapamientoYNoGuarda() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudDeNueveADoce()));
        when(repository.findDelCuidadorQueSeCruzan(1L, 1L, DIA, DIA, EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of());
        when(bloqueRepository.findByCuidadorIdAndFechaBetween(1L, DIA, DIA)).thenReturn(List.of(bloqueDePepe(8, 10)));

        assertThrows(SolicitudExceptionSolapamiento.class,
                () -> service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testAceptarConUnBloqueEnOtrasHorasEsValido() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudDeNueveADoce();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.findDelCuidadorQueSeCruzan(1L, 1L, DIA, DIA, EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of());
        when(bloqueRepository.findByCuidadorIdAndFechaBetween(1L, DIA, DIA)).thenReturn(List.of(bloqueDePepe(14, 20)));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        SolicitudDTOResponse resultado = service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

        assertThat(resultado.estado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
    }

    @Test
    void testAceptarConUnBloqueQueAcabaJustoCuandoEmpiezaElServicioEsValido() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudDeNueveADoce();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.findDelCuidadorQueSeCruzan(1L, 1L, DIA, DIA, EstadoSolicitud.ACEPTADA))
                .thenReturn(List.of());
        when(bloqueRepository.findByCuidadorIdAndFechaBetween(1L, DIA, DIA)).thenReturn(List.of(bloqueDePepe(6, 9)));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        SolicitudDTOResponse resultado = service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

        assertThat(resultado.estado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
    }

    @Test
    void testRechazarNoMiraLosBloques() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudDeNueveADoce();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        service.cambiarEstado(1L, EstadoSolicitud.RECHAZADA);

        verify(bloqueRepository, never()).findByCuidadorIdAndFechaBetween(Mockito.anyLong(),
                Mockito.any(LocalDate.class), Mockito.any(LocalDate.class));
    }
}
