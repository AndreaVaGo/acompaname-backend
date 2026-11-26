package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;

// Lo que se guarda al sincronizar: bloques nuevos, sin duplicar, sin pasados y sin tocar los de la persona
@ExtendWith(MockitoExtension.class)
public class GuardadoDeSincronizacionTest {

    @Mock
    private BloqueOcupadoRepository bloqueRepository;
    @Mock
    private SuscripcionCalendarioRepository suscripcionRepository;

    private GuardadoDeSincronizacion guardado;
    private PerfilCuidadorEntity pepe;
    private SuscripcionCalendarioEntity suscripcion;

    @BeforeEach
    void setup() {
        guardado = new GuardadoDeSincronizacion(bloqueRepository, suscripcionRepository);
        pepe = new PerfilCuidadorEntity();
        pepe.setId(4L);
        suscripcion = SuscripcionCalendarioEntity.builder().id(1L).cuidador(pepe)
                .url("https://calendar.google.com/x.ics").ultimoError("Falló antes").build();
    }

    private LecturaDeCalendario lectura(TramoOcupado... tramos) {
        return new LecturaDeCalendario(List.of(tramos), 0);
    }

    @Test
    void testBorraLosSincronizadosFuturosAntesDeCrearLosNuevos() {
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of());

        guardado.aplicar(suscripcion, lectura());

        verify(bloqueRepository).deleteByCuidadorIdAndMotivoAndFechaGreaterThanEqual(4L, "Calendario sincronizado",
                LocalDate.now());
        verify(bloqueRepository).flush();
    }

    @Test
    void testCreaLosBloquesConMotivoGenerico() {
        LocalDate dia = LocalDate.now().plusDays(3);
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of());
        when(bloqueRepository.save(any(BloqueOcupadoEntity.class))).thenAnswer(i -> i.getArgument(0));

        guardado.aplicar(suscripcion, lectura(new TramoOcupado(dia, LocalTime.of(9, 0), LocalTime.of(11, 0))));

        ArgumentCaptor<BloqueOcupadoEntity> captor = ArgumentCaptor.forClass(BloqueOcupadoEntity.class);
        verify(bloqueRepository).save(captor.capture());
        BloqueOcupadoEntity creado = captor.getValue();
        assertThat(creado.getCuidador(), is(equalTo(pepe)));
        assertThat(creado.getFecha(), is(equalTo(dia)));
        assertThat(creado.getHoraInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(creado.getHoraFin(), is(equalTo(LocalTime.of(11, 0))));
        assertThat(creado.getMotivo(), is(equalTo("Calendario sincronizado")));
    }

    @Test
    void testNoDuplicaUnBloqueQueLaPersonaYaTieneAMano() {
        LocalDate dia = LocalDate.now().plusDays(3);
        BloqueOcupadoEntity aMano = BloqueOcupadoEntity.builder().cuidador(pepe).fecha(dia)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0)).motivo("Mi turno").build();
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of(aMano));

        guardado.aplicar(suscripcion, lectura(new TramoOcupado(dia, LocalTime.of(9, 0), LocalTime.of(11, 0))));

        verify(bloqueRepository, never()).save(any(BloqueOcupadoEntity.class));
    }

    @Test
    void testNoDuplicaDosTramosIgualesDelMismoCalendario() {
        LocalDate dia = LocalDate.now().plusDays(3);
        TramoOcupado tramo = new TramoOcupado(dia, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of());
        when(bloqueRepository.save(any(BloqueOcupadoEntity.class))).thenAnswer(i -> i.getArgument(0));

        guardado.aplicar(suscripcion, lectura(tramo, tramo));

        verify(bloqueRepository, times(1)).save(any(BloqueOcupadoEntity.class));
    }

    @Test
    void testDejaFueraLosTramosPasados() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of());

        guardado.aplicar(suscripcion, lectura(new TramoOcupado(ayer, LocalTime.of(9, 0), LocalTime.of(11, 0))));

        verify(bloqueRepository, never()).save(any(BloqueOcupadoEntity.class));
    }

    @Test
    void testApuntaLaHoraYQuitaElErrorAnterior() {
        when(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of());

        guardado.aplicar(suscripcion, lectura());

        assertThat(suscripcion.getUltimaSincronizacion(), is(notNullValue()));
        assertThat(suscripcion.getUltimoError(), is(nullValue()));
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void testRegistrarErrorGuardaElMensajeSinTocarLosBloques() {
        guardado.registrarError(suscripcion, "No se pudo leer");

        assertThat(suscripcion.getUltimoError(), is(equalTo("No se pudo leer")));
        verify(suscripcionRepository).save(suscripcion);
        verify(bloqueRepository, never()).deleteByCuidadorIdAndMotivoAndFechaGreaterThanEqual(any(), any(), any());
    }

    @Test
    void testRegistrarErrorCortaLosMensajesLargos() {
        guardado.registrarError(suscripcion, "x".repeat(400));

        assertThat(suscripcion.getUltimoError().length(), is(255));
    }

    @Test
    void testRegistrarErrorSinMensajePoneUnoGenerico() {
        guardado.registrarError(suscripcion, null);

        assertThat(suscripcion.getUltimoError(), is(equalTo("No se pudo leer el calendario")));
    }

    @Test
    void testDesvincularBorraLaSuscripcionYTodosLosBloquesSincronizados() {
        guardado.desvincular(suscripcion);

        verify(bloqueRepository).deleteByCuidadorIdAndMotivo(4L, "Calendario sincronizado");
        verify(suscripcionRepository).delete(suscripcion);
    }
}
