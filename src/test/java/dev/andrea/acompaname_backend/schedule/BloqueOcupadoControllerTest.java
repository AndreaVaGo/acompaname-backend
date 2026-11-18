package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionNotFound;

@WebMvcTest(controllers = BloqueOcupadoController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BloqueOcupadoControllerTest {

    private static final String CUERPO_VALIDO = "{\"fecha\":\"2026-12-01\",\"horaInicio\":\"08:00\",\"horaFin\":\"15:00\",\"motivo\":\"Turno\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BloqueOcupadoService service;

    private MockHttpServletResponse enviar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/bloques-ocupados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    private BloqueOcupadoDTOResponse bloque() {
        return new BloqueOcupadoDTOResponse(1L, LocalDate.of(2026, 12, 1), LocalTime.of(8, 0), LocalTime.of(15, 0),
                "Turno");
    }

    @Test
    void testStoreDevuelve201ConElBloque() throws Exception {
        when(service.crear(ArgumentMatchers.any(BloqueOcupadoDTORequest.class))).thenReturn(bloque());

        MockHttpServletResponse response = enviar(CUERPO_VALIDO);

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("Turno"));
    }

    @Test
    void testStoreSinFechaDa400YNoGuarda() throws Exception {
        MockHttpServletResponse response = enviar("{\"horaInicio\":\"08:00\",\"horaFin\":\"15:00\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La fecha no puede ser nula"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreSinHorasDa400() throws Exception {
        MockHttpServletResponse response = enviar("{\"fecha\":\"2026-12-01\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La hora de inicio no puede ser nula"));
        assertThat(response.getContentAsString(), containsString("La hora de fin no puede ser nula"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreConMotivoDeMasDeCienCaracteresDa400() throws Exception {
        String cuerpo = "{\"fecha\":\"2026-12-01\",\"horaInicio\":\"08:00\",\"horaFin\":\"15:00\",\"motivo\":\""
                + "a".repeat(101) + "\"}";

        MockHttpServletResponse response = enviar(cuerpo);

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("no puede superar 100"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreSinMotivoEsValido() throws Exception {
        when(service.crear(ArgumentMatchers.any(BloqueOcupadoDTORequest.class))).thenReturn(bloque());

        MockHttpServletResponse response = enviar(
                "{\"fecha\":\"2026-12-01\",\"horaInicio\":\"08:00\",\"horaFin\":\"15:00\"}");

        assertThat(response.getStatus(), is(equalTo(201)));
    }

    @Test
    void testStoreSiElServicioDiceFechaInvalidaDa400ConElMensaje() throws Exception {
        when(service.crear(ArgumentMatchers.any(BloqueOcupadoDTORequest.class)))
                .thenThrow(new BloqueOcupadoExceptionFechaInvalida("La fecha no puede estar en el pasado"));

        MockHttpServletResponse response = enviar(CUERPO_VALIDO);

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("en el pasado"));
    }

    @Test
    void testIndexDevuelve200ConLaLista() throws Exception {
        when(service.listarMios()).thenReturn(List.of(bloque()));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/bloques-ocupados"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("Turno"));
    }

    @Test
    void testDestroyDevuelve204() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/bloques-ocupados/1"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).borrar(1L);
    }

    @Test
    void testDestroyDeUnBloqueQueNoExisteDa404() throws Exception {
        doThrow(new BloqueOcupadoExceptionNotFound("Bloque no encontrado")).when(service).borrar(99L);

        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/bloques-ocupados/99"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(404)));
    }

    @Test
    void testDestroyDeUnBloqueAjenoDa403() throws Exception {
        doThrow(new BloqueOcupadoExceptionAccesoDenegado("No tienes permiso")).when(service).borrar(5L);

        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/bloques-ocupados/5"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(403)));
    }
}
