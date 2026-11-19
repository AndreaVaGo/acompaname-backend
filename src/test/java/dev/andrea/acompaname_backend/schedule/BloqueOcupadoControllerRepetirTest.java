package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.schedule.dtos.RepeticionBloquesDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

@WebMvcTest(controllers = BloqueOcupadoController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BloqueOcupadoControllerRepetirTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BloqueOcupadoService service;

    private static final String CUERPO_VALIDO = "{\"desde\":\"2030-01-07\",\"hasta\":\"2030-01-31\","
            + "\"diasSemana\":[\"MONDAY\",\"TUESDAY\"],\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\","
            + "\"motivo\":\"Turno de mañana\"}";

    private MockHttpServletResponse repetir(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/bloques-ocupados/repetir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testRepetirDevuelve201ConElResumen() throws Exception {
        when(service.repetir(ArgumentMatchers.any())).thenReturn(new RepeticionBloquesDTOResponse(7, 2));

        MockHttpServletResponse response = repetir(CUERPO_VALIDO);

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("\"creados\":7"));
        assertThat(response.getContentAsString(), containsString("\"duplicados\":2"));
    }

    @Test
    void testRepetirPasaLosDatosAlServicio() throws Exception {
        when(service.repetir(ArgumentMatchers.any())).thenReturn(new RepeticionBloquesDTOResponse(0, 0));

        repetir(CUERPO_VALIDO);

        Mockito.verify(service).repetir(ArgumentMatchers.argThat(dto -> dto.desde().toString().equals("2030-01-07")
                && dto.hasta().toString().equals("2030-01-31")
                && dto.diasSemana().size() == 2
                && dto.horaInicio().toString().equals("07:00")
                && dto.horaFin().toString().equals("15:00")
                && "Turno de mañana".equals(dto.motivo())));
    }

    @Test
    void testRepetirSinDiasDeLaSemanaDa400() throws Exception {
        MockHttpServletResponse response = repetir("{\"desde\":\"2030-01-07\",\"hasta\":\"2030-01-31\","
                + "\"diasSemana\":[],\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testRepetirSinFechasDa400() throws Exception {
        MockHttpServletResponse response = repetir("{\"diasSemana\":[\"MONDAY\"],"
                + "\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testRepetirSinHorasDa400() throws Exception {
        MockHttpServletResponse response = repetir("{\"desde\":\"2030-01-07\",\"hasta\":\"2030-01-31\","
                + "\"diasSemana\":[\"MONDAY\"]}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testRepetirConUnMotivoMuyLargoDa400() throws Exception {
        String motivoLargo = "a".repeat(101);
        MockHttpServletResponse response = repetir("{\"desde\":\"2030-01-07\",\"hasta\":\"2030-01-31\","
                + "\"diasSemana\":[\"MONDAY\"],\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\","
                + "\"motivo\":\"" + motivoLargo + "\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testRepetirConUnDiaDeLaSemanaInventadoDa400() throws Exception {
        MockHttpServletResponse response = repetir("{\"desde\":\"2030-01-07\",\"hasta\":\"2030-01-31\","
                + "\"diasSemana\":[\"FUNDAY\"],\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testRepetirConFechasQueNoValenDa400ConElMensaje() throws Exception {
        when(service.repetir(ArgumentMatchers.any())).thenThrow(
                new BloqueOcupadoExceptionFechaInvalida("La fecha de inicio no puede estar en el pasado"));

        MockHttpServletResponse response = repetir(CUERPO_VALIDO);

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("en el pasado"));
    }
}
