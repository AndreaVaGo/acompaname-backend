package dev.andrea.acompaname_backend.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.review.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.review.mappers.ValoracionMapper;

// La fecha de una valoración la pone el servidor: siempre es la de hoy
public class ValoracionMapperTest {

    @Test
    void testLaFechaDeLaValoracionNuevaEsLaDeHoy() {
        ValoracionDTORequest dto = new ValoracionDTORequest("Muy bien", 5, 1L);

        ValoracionEntity valoracion = ValoracionMapper.toEntity(dto, null);

        assertThat(valoracion.getFecha(), is(equalTo(LocalDate.now())));
    }
}
