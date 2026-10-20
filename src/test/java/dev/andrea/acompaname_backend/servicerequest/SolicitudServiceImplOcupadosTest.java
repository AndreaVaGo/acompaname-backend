package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la búsqueda de cuidadores ocupados en unas fechas (para filtrar en la búsqueda de la familia).
// Qué fechas se cruzan lo decide la consulta del repositorio, que se prueba con MySQL real
// en SolicitudRepositoryIntegrationTest. Aquí se prueba lo que hace el servicio.
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplOcupadosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;

    private SolicitudServiceImpl service;

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository);
    }

    @Test
    void testPideAlRepositorioLosCuidadoresOcupadosConServicioAceptadoEnEseRango() {
        when(repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11),
                EstadoSolicitud.ACEPTADA)).thenReturn(List.of(1L));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSinFechaFinSeBuscaUnSoloDia() {
        when(repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10),
                EstadoSolicitud.ACEPTADA)).thenReturn(List.of(1L));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSiNadieEstaOcupadoDevuelveListaVacia() {
        when(repository.findCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                EstadoSolicitud.ACEPTADA)).thenReturn(List.of());

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testConFechaFinAnteriorALaDeInicioDaFechaInvalidaYNoConsultaLaBaseDeDatos() {
        assertThrows(SolicitudExceptionFechaInvalida.class,
                () -> service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 5)));
        verifyNoInteractions(repository);
    }
}