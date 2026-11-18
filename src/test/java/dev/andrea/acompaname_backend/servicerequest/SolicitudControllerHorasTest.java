package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import tools.jackson.databind.ObjectMapper;

// Tests de las horas de la solicitud en el controlador: son obligatorias al crearla
@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerHorasTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService service;
    @Autowired
    ObjectMapper mapper;

    private SolicitudDTOResponse crearResponse() {
        return new SolicitudDTOResponse(1L, "domicilio", "Manuel", "Sin notas", 80, LocalDate.of(2030, 1, 10), null,
                EstadoSolicitud.PENDIENTE, 1L, 1L, "Ana", "Pepe", new ArrayList<>(), null, null,
                LocalTime.of(9, 0), LocalTime.of(12, 0));
    }

    @Test
    void testStorePasaLasHorasAlServicioYDevuelveLasHoras() throws Exception {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.now().plusDays(5), null, 1L, 1L, null, null, null, LocalTime.of(9, 0),
                LocalTime.of(12, 0));
        when(service.storeEntity(any(SolicitudDTORequest.class))).thenReturn(crearResponse());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<SolicitudDTORequest> captor = ArgumentCaptor.forClass(SolicitudDTORequest.class);
        verify(service).storeEntity(captor.capture());
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(captor.getValue().horaInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(captor.getValue().horaFin(), is(equalTo(LocalTime.of(12, 0))));
        assertThat(response.getContentAsString(), containsString("\"horaInicio\":\"09:00"));
        assertThat(response.getContentAsString(), containsString("\"horaFin\":\"12:00"));
    }

    @Test
    void testStoreSinHorasDevuelve400() throws Exception {
        String json = "{\"tipoCuidado\":\"domicilio\",\"nombrePaciente\":\"Manuel\",\"edadPaciente\":80,"
                + "\"fechaCuidado\":\"2030-01-10\",\"familiaId\":1,\"cuidadorId\":1}";

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La hora de inicio no puede ser nula"));
        assertThat(response.getContentAsString(), containsString("La hora de fin no puede ser nula"));
        verify(service, never()).storeEntity(any(SolicitudDTORequest.class));
    }

    @Test
    void testStoreConUnaHoraQueNoExisteDevuelve400() throws Exception {
        String json = "{\"tipoCuidado\":\"domicilio\",\"nombrePaciente\":\"Manuel\",\"edadPaciente\":80,"
                + "\"fechaCuidado\":\"2030-01-10\",\"familiaId\":1,\"cuidadorId\":1,"
                + "\"horaInicio\":\"25:99\",\"horaFin\":\"12:00\"}";

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(SolicitudDTORequest.class));
    }
}
