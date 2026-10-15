package dev.andrea.acompaname_backend.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.review.dtos.ValoracionPublicaDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
public class ValoracionServiceImplPublicasTest {

    private ValoracionServiceImpl service;

    @Mock
    private ValoracionRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;

    @BeforeEach
    void setup() {
        service = new ValoracionServiceImpl(repository, solicitudRepository);
    }

    @Test
    void testDevuelvePuntuacionComentarioYFecha() {
        List<ValoracionEntity> valoraciones = List.of(
                new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11), null),
                new ValoracionEntity(2L, "Puntual", 4, LocalDate.of(2026, 9, 5), null));
        when(repository.findBySolicitudCuidadorIdOrderByFechaDesc(3L)).thenReturn(valoraciones);

        List<ValoracionPublicaDTOResponse> resultado = service.getPublicasPorCuidador(3L);

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).puntuacion(), is(equalTo(5)));
        assertThat(resultado.get(0).comentario(), is(equalTo("Muy buena atención")));
        assertThat(resultado.get(0).fecha(), is(equalTo(LocalDate.of(2026, 9, 11))));
        assertThat(resultado.get(1).comentario(), is(equalTo("Puntual")));
    }

    @Test
    void testPideLasValoracionesDelCuidadorIndicado() {
        when(repository.findBySolicitudCuidadorIdOrderByFechaDesc(7L)).thenReturn(List.of());

        service.getPublicasPorCuidador(7L);

        verify(repository).findBySolicitudCuidadorIdOrderByFechaDesc(7L);
    }

    @Test
    void testCuidadorSinValoracionesDevuelveListaVacia() {
        when(repository.findBySolicitudCuidadorIdOrderByFechaDesc(9L)).thenReturn(List.of());

        List<ValoracionPublicaDTOResponse> resultado = service.getPublicasPorCuidador(9L);

        assertThat(resultado, is(empty()));
    }
}