package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.schedule.dtos.SuscripcionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

@WebMvcTest(controllers = SuscripcionCalendarioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SuscripcionCalendarioControllerTest {

    private static final String RUTA = "/api/v1/bloques-ocupados/suscripcion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SuscripcionCalendarioService service;

    @Test
    void testEstadoDevuelve200() throws Exception {
        when(service.estado()).thenReturn(SuscripcionCalendarioDTOResponse.noVinculado());

        MockHttpServletResponse response = mockMvc.perform(get(RUTA)).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"vinculado\":false"));
    }

    @Test
    void testVincularDevuelve200ConElEstadoYSinLaDireccion() throws Exception {
        when(service.vincular(anyString())).thenReturn(
                new SuscripcionCalendarioDTOResponse(true, LocalDateTime.of(2026, 11, 26, 10, 0), null));

        MockHttpServletResponse response = mockMvc.perform(put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://calendar.google.com/secreto.ics\"}")).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"vinculado\":true"));
        assertThat(response.getContentAsString().contains("secreto"), is(false));
        verify(service).vincular("https://calendar.google.com/secreto.ics");
    }

    @Test
    void testVincularSinDireccionDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"  \"}")).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
    }

    @Test
    void testVincularConDireccionMuyLargaDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://calendar.google.com/" + "a".repeat(600) + "\"}")).andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
    }

    @Test
    void testVincularUnaDireccionNoPermitidaDevuelve400ConElMensaje() throws Exception {
        when(service.vincular(anyString()))
                .thenThrow(new BloqueOcupadoExceptionFechaInvalida("Solo se pueden vincular calendarios de Google"));

        MockHttpServletResponse response = mockMvc.perform(put(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/x.ics\"}")).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("Solo se pueden vincular"));
    }

    @Test
    void testSincronizarDevuelve200() throws Exception {
        when(service.sincronizarAhora()).thenReturn(new SuscripcionCalendarioDTOResponse(true, null, "Falló"));

        MockHttpServletResponse response = mockMvc.perform(post(RUTA + "/sincronizar")).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"ultimoError\":\"Falló\""));
    }

    @Test
    void testDesvincularDevuelve204() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete(RUTA)).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        verify(service).desvincular();
    }
}
