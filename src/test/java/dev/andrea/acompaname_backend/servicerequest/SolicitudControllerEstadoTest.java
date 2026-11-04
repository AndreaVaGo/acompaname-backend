package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerEstadoTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testCambiarAUnEstadoNoPermitidoDevuelve409ConElMensaje() throws Exception {
        when(service.cambiarEstado(1L, EstadoSolicitud.COMPLETADA)).thenThrow(
                new SolicitudExceptionEstadoInvalido("No se puede pasar una solicitud de PENDIENTE a COMPLETADA"));

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/solicitudes/1/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"COMPLETADA\"}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(409)));
        assertThat(response.getContentAsString(), containsString("PENDIENTE a COMPLETADA"));
    }

    @Test
    void testEditarUnaSolicitudQueYaNoEstaPendienteDevuelve409() throws Exception {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.now().plusDays(5), 1L, 1L);
        when(service.update(ArgumentMatchers.eq(1L), ArgumentMatchers.any(SolicitudDTORequest.class)))
                .thenThrow(new SolicitudExceptionEstadoInvalido("Solo se puede modificar una solicitud pendiente"));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/solicitudes/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(409)));
        assertThat(response.getContentAsString(), containsString("solicitud pendiente"));
    }
}