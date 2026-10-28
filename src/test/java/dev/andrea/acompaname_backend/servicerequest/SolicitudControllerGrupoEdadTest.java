package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
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

import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerGrupoEdadTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService service;
    @Autowired
    ObjectMapper mapper;

    // Respuesta con el grupo de edad que le pasemos
    private SolicitudDTOResponse crearResponse(GrupoEdad grupo) {
        return new SolicitudDTOResponse(1L, "domicilio", "Manuel", "Sin notas", 12, LocalDate.of(2030, 1, 10), null,
                EstadoSolicitud.PENDIENTE, 1L, 1L, "Ana", "Pepe", new ArrayList<>(), null, grupo);
    }

    @Test
    void testGetByIdDevuelveElGrupoDeEdad() throws Exception {
        when(service.getById(1L)).thenReturn(crearResponse(GrupoEdad.NINOS));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"grupoEdad\":\"NINOS\""));
    }

    @Test
    void testStorePasaElGrupoDeEdadAlServicio() throws Exception {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 12,
                LocalDate.now().plusDays(5), null, 1L, 1L, null, null, GrupoEdad.NINOS);
        when(service.storeEntity(any(SolicitudDTORequest.class))).thenReturn(crearResponse(GrupoEdad.NINOS));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<SolicitudDTORequest> captor = ArgumentCaptor.forClass(SolicitudDTORequest.class);
        verify(service).storeEntity(captor.capture());
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(captor.getValue().grupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testStoreConUnGrupoQueNoExisteDevuelve400() throws Exception {
        String json = "{\"tipoCuidado\":\"domicilio\",\"nombrePaciente\":\"Manuel\",\"edadPaciente\":12,"
                + "\"fechaCuidado\":\"2030-01-10\",\"familiaId\":1,\"cuidadorId\":1,\"grupoEdad\":\"VIEJOS\"}";

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(SolicitudDTORequest.class));
    }
}