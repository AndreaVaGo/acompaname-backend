package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.dtos.RepeticionBloquesDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.RepetirBloquesDTORequest;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Repetir un rato ocupado (turnos): crea un bloque por cada día elegido, parte las noches en dos y no duplica
@ExtendWith(MockitoExtension.class)
public class BloqueOcupadoServiceImplRepetirTest {

    private static final Set<DayOfWeek> LUNES_A_VIERNES = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    @Mock
    private BloqueOcupadoRepository repository;
    @Mock
    private PerfilCuidadorRepository perfilRepository;

    private BloqueOcupadoServiceImpl service;
    private PerfilCuidadorEntity pepe;
    private LocalDate proximoLunes;

    @BeforeEach
    void setup() {
        service = new BloqueOcupadoServiceImpl(repository, perfilRepository);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("pepe@test.com", null));
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmail("pepe@test.com");
        pepe = new PerfilCuidadorEntity();
        pepe.setId(4L);
        pepe.setUsuario(usuario);
        // Un lunes que seguro está en el futuro, para saber qué día de la semana es cada fecha
        proximoLunes = LocalDate.now().plusDays(1);
        while (proximoLunes.getDayOfWeek() != DayOfWeek.MONDAY) {
            proximoLunes = proximoLunes.plusDays(1);
        }
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void darPerfil() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(pepe));
    }

    private void guardarDevuelveLoGuardado() {
        when(repository.save(ArgumentMatchers.any(BloqueOcupadoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RepetirBloquesDTORequest peticion(LocalDate desde, LocalDate hasta, Set<DayOfWeek> dias,
            LocalTime inicio, LocalTime fin, String motivo) {
        return new RepetirBloquesDTORequest(desde, hasta, dias, inicio, fin, motivo);
    }

    private List<BloqueOcupadoEntity> bloquesGuardados(int veces) {
        ArgumentCaptor<BloqueOcupadoEntity> captor = ArgumentCaptor.forClass(BloqueOcupadoEntity.class);
        verify(repository, times(veces)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void testDeLunesAViernesCreaUnBloqueEnCadaDiaDeLaSemana() {
        darPerfil();
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes.plusDays(6),
                LUNES_A_VIERNES, LocalTime.of(7, 0), LocalTime.of(15, 0), "Turno de mañana"));

        assertThat(resumen.creados(), is(equalTo(5)));
        assertThat(resumen.duplicados(), is(equalTo(0)));
        List<BloqueOcupadoEntity> guardados = bloquesGuardados(5);
        for (int i = 0; i < 5; i++) {
            assertThat(guardados.get(i).getFecha(), is(equalTo(proximoLunes.plusDays(i))));
            assertThat(guardados.get(i).getHoraInicio(), is(equalTo(LocalTime.of(7, 0))));
            assertThat(guardados.get(i).getHoraFin(), is(equalTo(LocalTime.of(15, 0))));
            assertThat(guardados.get(i).getMotivo(), is(equalTo("Turno de mañana")));
            assertThat(guardados.get(i).getCuidador(), is(equalTo(pepe)));
        }
    }

    @Test
    void testSoloCreaLosDiasDeLaSemanaElegidos() {
        darPerfil();
        guardarDevuelveLoGuardado();

        // Dos semanas, solo lunes y miércoles: 4 bloques
        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes.plusDays(13),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(15, 0), LocalTime.of(22, 0), null));

        assertThat(resumen.creados(), is(equalTo(4)));
        for (BloqueOcupadoEntity bloque : bloquesGuardados(4)) {
            DayOfWeek dia = bloque.getFecha().getDayOfWeek();
            assertThat(dia == DayOfWeek.MONDAY || dia == DayOfWeek.WEDNESDAY, is(true));
        }
    }

    @Test
    void testLaFechaDeInicioYLaDeFinCuentanComoDiasElegidos() {
        darPerfil();
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes,
                Set.of(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        assertThat(resumen.creados(), is(equalTo(1)));
    }

    @Test
    void testUnTurnoDeNocheSeGuardaEnDosBloquesUnoCadaDia() {
        darPerfil();
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes,
                Set.of(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(7, 0), "Turno de noche"));

        assertThat(resumen.creados(), is(equalTo(2)));
        List<BloqueOcupadoEntity> guardados = bloquesGuardados(2);
        assertThat(guardados.get(0).getFecha(), is(equalTo(proximoLunes)));
        assertThat(guardados.get(0).getHoraInicio(), is(equalTo(LocalTime.of(22, 0))));
        assertThat(guardados.get(0).getHoraFin(), is(equalTo(LocalTime.of(23, 59))));
        assertThat(guardados.get(1).getFecha(), is(equalTo(proximoLunes.plusDays(1))));
        assertThat(guardados.get(1).getHoraInicio(), is(equalTo(LocalTime.MIDNIGHT)));
        assertThat(guardados.get(1).getHoraFin(), is(equalTo(LocalTime.of(7, 0))));
    }

    @Test
    void testUnaNocheQueAcabaALaMedianocheSoloCreaUnBloque() {
        darPerfil();
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes,
                Set.of(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.MIDNIGHT, null));

        assertThat(resumen.creados(), is(equalTo(1)));
        BloqueOcupadoEntity bloque = bloquesGuardados(1).get(0);
        assertThat(bloque.getHoraInicio(), is(equalTo(LocalTime.of(22, 0))));
        assertThat(bloque.getHoraFin(), is(equalTo(LocalTime.of(23, 59))));
    }

    @Test
    void testNoCreaLosBloquesQueYaExisten() {
        darPerfil();
        BloqueOcupadoEntity yaEstaba = BloqueOcupadoEntity.builder().cuidador(pepe).fecha(proximoLunes)
                .horaInicio(LocalTime.of(7, 0)).horaFin(LocalTime.of(15, 0)).motivo("Otro").build();
        when(repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of(yaEstaba));
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes.plusDays(1),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), LocalTime.of(7, 0), LocalTime.of(15, 0), null));

        assertThat(resumen.creados(), is(equalTo(1)));
        assertThat(resumen.duplicados(), is(equalTo(1)));
        assertThat(bloquesGuardados(1).get(0).getFecha(), is(equalTo(proximoLunes.plusDays(1))));
    }

    @Test
    void testElMotivoSeGuardaSinEspaciosYSiEstaEnBlancoNoSeGuarda() {
        darPerfil();
        guardarDevuelveLoGuardado();

        service.repetir(peticion(proximoLunes, proximoLunes.plusDays(1), Set.of(DayOfWeek.MONDAY),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "   "));
        service.repetir(peticion(proximoLunes, proximoLunes.plusDays(1), Set.of(DayOfWeek.MONDAY),
                LocalTime.of(11, 0), LocalTime.of(12, 0), "  Hospital  "));

        List<BloqueOcupadoEntity> guardados = bloquesGuardados(2);
        assertThat(guardados.get(0).getMotivo(), is(nullValue()));
        assertThat(guardados.get(1).getMotivo(), is(equalTo("Hospital")));
    }

    @Test
    void testSinPerfilDeCuidadorNoSeCreaNada() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.repetir(peticion(proximoLunes,
                proximoLunes, Set.of(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(10, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testUnaFechaDeInicioEnElPasadoDaError() {
        darPerfil();

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.repetir(peticion(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), LUNES_A_VIERNES,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testLaFechaDeFinAnteriorALaDeInicioDaError() {
        darPerfil();

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.repetir(peticion(
                proximoLunes.plusDays(3), proximoLunes, LUNES_A_VIERNES,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testRepetirMasDeSeisMesesDaError() {
        darPerfil();

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.repetir(peticion(
                proximoLunes, proximoLunes.plusDays(184), LUNES_A_VIERNES,
                LocalTime.of(9, 0), LocalTime.of(10, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testSeisMesesJustosSiSePuedeRepetir() {
        darPerfil();
        guardarDevuelveLoGuardado();

        RepeticionBloquesDTOResponse resumen = service.repetir(peticion(proximoLunes, proximoLunes.plusDays(183),
                LUNES_A_VIERNES, LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        // 184 días: 26 semanas completas (130 días laborables) más un lunes y un martes
        assertThat(resumen.creados(), is(equalTo(132)));
    }

    @Test
    void testLaHoraDeFinIgualALaDeInicioDaError() {
        darPerfil();

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.repetir(peticion(
                proximoLunes, proximoLunes, Set.of(DayOfWeek.MONDAY),
                LocalTime.of(9, 0), LocalTime.of(9, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }

    @Test
    void testSiNingunDiaElegidoCaeEntreLasFechasDaError() {
        darPerfil();

        // Solo un lunes, pero se pide martes
        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.repetir(peticion(
                proximoLunes, proximoLunes, Set.of(DayOfWeek.TUESDAY),
                LocalTime.of(9, 0), LocalTime.of(10, 0), null)));

        verify(repository, never()).save(ArgumentMatchers.any());
    }
}
