package dev.andrea.acompaname_backend.verificacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.verificacion.dtos.VerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificadosDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = VerificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class VerificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private VerificacionService service;
    @Autowired
    ObjectMapper mapper;

    private VerificacionDTOResponse ejemplo() {
        return new VerificacionDTOResponse(1L, TipoVerificacion.TITULACION, EstadoVerificacion.PENDIENTE, "12345",
                LocalDateTime.of(2026, 11, 22, 10, 0), null, null);
    }

    @Test
    void testPublicasDevuelveSoloLosSiNo() throws Exception {
        List<VerificadosDTOResponse> lista = List.of(new VerificadosDTOResponse(1L, true, false));
        when(service.publicas()).thenReturn(lista);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/verificaciones/publicas"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(lista))));
    }

    @Test
    void testMiasDevuelveMisVerificaciones() throws Exception {
        List<VerificacionDTOResponse> lista = List.of(ejemplo());
        when(service.listarMias()).thenReturn(lista);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/verificaciones/mias"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(lista))));
    }

    @Test
    void testEnviarDevuelveLaVerificacionPendiente() throws Exception {
        when(service.enviar(TipoVerificacion.TITULACION, "12345")).thenReturn(ejemplo());

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"12345\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(ejemplo()))));
    }

    @Test
    void testEnviarConReferenciaVaciaDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"   \"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).enviar(any(), anyString());
    }

    @Test
    void testEnviarConReferenciaMuyLargaDevuelve400() throws Exception {
        String larga = "a".repeat(101);

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/verificaciones/ANTECEDENTES")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"" + larga + "\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).enviar(any(), anyString());
    }

    @Test
    void testEnviarConTipoDesconocidoDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/verificaciones/CARNET")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"12345\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testEnviarYaVerificadaDevuelve400ConElMensaje() throws Exception {
        when(service.enviar(TipoVerificacion.TITULACION, "12345"))
                .thenThrow(new VerificacionException("Esta verificación ya está comprobada"));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"12345\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), is(equalTo("Esta verificación ya está comprobada")));
    }
}
