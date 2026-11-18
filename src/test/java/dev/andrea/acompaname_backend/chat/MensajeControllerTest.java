package dev.andrea.acompaname_backend.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionConflicto;

@WebMvcTest(controllers = MensajeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MensajeService service;

    private MensajeDTOResponse respuesta(Long id, String texto) {
        return new MensajeDTOResponse(id, 5L, 1L, "Ana", texto, LocalDateTime.of(2026, 11, 18, 10, 0), true);
    }

    @Test
    void testIndexDevuelveLosMensajes() throws Exception {
        when(service.getMensajes(5L, null)).thenReturn(List.of(respuesta(1L, "Hola Pepe")));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/5/mensajes"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("Hola Pepe"));
    }

    @Test
    void testIndexPasaElParametroDesdeAlServicio() throws Exception {
        when(service.getMensajes(5L, 3L)).thenReturn(List.of());

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/5/mensajes?desde=3"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        verify(service).getMensajes(5L, 3L);
    }

    @Test
    void testIndexDeUnaSolicitudAjenaDevuelve403() throws Exception {
        when(service.getMensajes(5L, null)).thenThrow(new ChatExceptionAccesoDenegado("No permitido"));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/5/mensajes"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(403)));
    }

    @Test
    void testStoreDevuelve201ConElMensajeCreado() throws Exception {
        when(service.enviar(eq(5L), any(MensajeDTORequest.class))).thenReturn(respuesta(2L, "Buenas"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes/5/mensajes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Buenas\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("Buenas"));
    }

    @Test
    void testStoreConTextoVacioDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes/5/mensajes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"   \"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).enviar(any(), any());
    }

    @Test
    void testStoreConTextoDemasiadoLargoDevuelve400() throws Exception {
        String largo = "a".repeat(1001);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes/5/mensajes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + largo + "\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).enviar(any(), any());
    }

    @Test
    void testStoreEnUnaSolicitudRechazadaDevuelve409() throws Exception {
        when(service.enviar(eq(5L), any(MensajeDTORequest.class)))
                .thenThrow(new ChatExceptionConflicto("Solicitud rechazada"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes/5/mensajes")
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(409)));
    }
}
