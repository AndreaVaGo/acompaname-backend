package dev.andrea.acompaname_backend.solicitud;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
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

import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndex() throws Exception {
        SolicitudDTOResponse solicitud = new SolicitudDTOResponse(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, 1L, 1L);
        List<SolicitudDTOResponse> solicitudes = new ArrayList<>();
        solicitudes.add(solicitud);
        String json = mapper.writeValueAsString(solicitudes);
        when(service.getEntities()).thenReturn(solicitudes);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Acompañamiento"));
    }

    @Test
    void testGetById() throws Exception {
        SolicitudDTOResponse solicitud = new SolicitudDTOResponse(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, 1L, 1L);
        String json = mapper.writeValueAsString(solicitud);
        when(service.getById(1L)).thenReturn(solicitud);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Acompañamiento"));
    }

    @Test
    void testStore() throws Exception {
        SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), 1L, 1L);
        SolicitudDTOResponse dtoResponse = new SolicitudDTOResponse(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, 1L, 1L);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(SolicitudDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Acompañamiento"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/solicitudes/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }
}