package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
import dev.andrea.acompaname_backend.schedule.dtos.ImportacionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Importar un calendario: crea los bloques nuevos, salta los repetidos y los pasados y cuenta lo que hace
@ExtendWith(MockitoExtension.class)
public class BloqueOcupadoServiceImplImportarTest {

    @Mock
    private BloqueOcupadoRepository repository;
    @Mock
    private PerfilCuidadorRepository perfilRepository;

    private BloqueOcupadoServiceImpl service;
    private PerfilCuidadorEntity pepe;

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
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private String fecha(LocalDate dia) {
        return dia.toString().replace("-", "");
    }

    // Un calendario con un único evento de 09:00 a 11:00 (hora local) ese día
    private String calendarioDeUnEvento(LocalDate dia) {
        return "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Cita privada\r\nDTSTART:" + fecha(dia)
                + "T090000\r\nDTEND:" + fecha(dia) + "T110000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
    }

    private void darPerfil() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(pepe));
    }

    private void guardarDevuelveLoGuardado() {
        when(repository.save(ArgumentMatchers.any(BloqueOcupadoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testImportaElEventoComoBloqueDeLaPersonaLogueada() {
        darPerfil();
        guardarDevuelveLoGuardado();
        LocalDate dia = LocalDate.now().plusDays(5);

        ImportacionCalendarioDTOResponse resumen = service.importar(calendarioDeUnEvento(dia));

        ArgumentCaptor<BloqueOcupadoEntity> captor = ArgumentCaptor.forClass(BloqueOcupadoEntity.class);
        verify(repository).save(captor.capture());
        BloqueOcupadoEntity guardado = captor.getValue();
        assertThat(guardado.getCuidador(), is(equalTo(pepe)));
        assertThat(guardado.getFecha(), is(equalTo(dia)));
        assertThat(guardado.getHoraInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(guardado.getHoraFin(), is(equalTo(LocalTime.of(11, 0))));
        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(1, 0, 0))));
    }

    @Test
    void testElMotivoEsGenericoYNuncaElTituloDeLaCita() {
        darPerfil();
        guardarDevuelveLoGuardado();

        service.importar(calendarioDeUnEvento(LocalDate.now().plusDays(5)));

        ArgumentCaptor<BloqueOcupadoEntity> captor = ArgumentCaptor.forClass(BloqueOcupadoEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMotivo(), is(equalTo("Calendario importado")));
    }

    @Test
    void testNoCreaUnBloqueQueYaExiste() {
        darPerfil();
        LocalDate dia = LocalDate.now().plusDays(5);
        BloqueOcupadoEntity yaGuardado = BloqueOcupadoEntity.builder().cuidador(pepe).fecha(dia)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0)).build();
        when(repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of(yaGuardado));

        ImportacionCalendarioDTOResponse resumen = service.importar(calendarioDeUnEvento(dia));

        verify(repository, never()).save(ArgumentMatchers.any(BloqueOcupadoEntity.class));
        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(0, 1, 0))));
    }

    @Test
    void testUnBloqueConOtraHoraNoCuentaComoRepetido() {
        darPerfil();
        guardarDevuelveLoGuardado();
        LocalDate dia = LocalDate.now().plusDays(5);
        BloqueOcupadoEntity otroRato = BloqueOcupadoEntity.builder().cuidador(pepe).fecha(dia)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0)).build();
        when(repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of(otroRato));

        ImportacionCalendarioDTOResponse resumen = service.importar(calendarioDeUnEvento(dia));

        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(1, 0, 0))));
    }

    @Test
    void testSaltaLosEventosQueYaHanPasado() {
        darPerfil();
        LocalDate ayer = LocalDate.now().minusDays(1);

        ImportacionCalendarioDTOResponse resumen = service.importar(calendarioDeUnEvento(ayer));

        verify(repository, never()).save(ArgumentMatchers.any(BloqueOcupadoEntity.class));
        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(0, 0, 1))));
    }

    @Test
    void testUnEventoDeHoyTodaviaSeImporta() {
        darPerfil();
        guardarDevuelveLoGuardado();

        ImportacionCalendarioDTOResponse resumen = service.importar(calendarioDeUnEvento(LocalDate.now()));

        assertThat(resumen.importados(), is(equalTo(1)));
    }

    @Test
    void testDosEventosIgualesEnElMismoArchivoSoloCreanUnBloque() {
        darPerfil();
        guardarDevuelveLoGuardado();
        LocalDate dia = LocalDate.now().plusDays(5);
        String evento = "BEGIN:VEVENT\r\nDTSTART:" + fecha(dia) + "T090000\r\nDTEND:" + fecha(dia)
                + "T110000\r\nEND:VEVENT\r\n";
        String ics = "BEGIN:VCALENDAR\r\n" + evento + evento + "END:VCALENDAR\r\n";

        ImportacionCalendarioDTOResponse resumen = service.importar(ics);

        verify(repository, times(1)).save(ArgumentMatchers.any(BloqueOcupadoEntity.class));
        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(1, 1, 0))));
    }

    @Test
    void testSumaALosOmitidosLosEventosRepetidosDelArchivo() {
        darPerfil();
        LocalDate dia = LocalDate.now().plusDays(5);
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nDTSTART:" + fecha(dia) + "T090000\r\nDTEND:" + fecha(dia)
                + "T110000\r\nRRULE:FREQ=WEEKLY\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

        ImportacionCalendarioDTOResponse resumen = service.importar(ics);

        assertThat(resumen, is(equalTo(new ImportacionCalendarioDTOResponse(0, 0, 1))));
    }

    @Test
    void testSinPerfilDeCuidadorDaNotFound() {
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class,
                () -> service.importar(calendarioDeUnEvento(LocalDate.now().plusDays(5))));
        verify(repository, never()).save(ArgumentMatchers.any(BloqueOcupadoEntity.class));
    }

    @Test
    void testUnArchivoQueNoEsUnCalendarioDaErrorYNoGuardaNada() {
        darPerfil();

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service.importar("esto no es un .ics"));
        verify(repository, never()).save(ArgumentMatchers.any(BloqueOcupadoEntity.class));
    }
}
