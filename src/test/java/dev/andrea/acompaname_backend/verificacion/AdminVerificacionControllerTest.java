package dev.andrea.acompaname_backend.verificacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

import dev.andrea.acompaname_backend.verificacion.dtos.AdminVerificacionDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminVerificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminVerificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private VerificacionService service;
    @Autowired
    ObjectMapper mapper;

    private AdminVerificacionDTOResponse ejemplo(EstadoVerificacion estado) {
        return new AdminVerificacionDTOResponse(1L, 3L, "Pepe", TipoVerificacion.ANTECEDENTES, estado, "SD:ABC123",
                LocalDateTime.of(2026, 11, 22, 10, 0), null, null);
    }

    @Test
    void testListarSinEstadoPideTodas() throws Exception {
        List<AdminVerificacionDTOResponse> lista = List.of(ejemplo(EstadoVerificacion.PENDIENTE));
        when(service.listarParaAdmin(null)).thenReturn(lista);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/verificaciones"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(lista))));
    }

    @Test
    void testListarConEstadoFiltra() throws Exception {
        List<AdminVerificacionDTOResponse> lista = List.of(ejemplo(EstadoVerificacion.PENDIENTE));
        when(service.listarParaAdmin(EstadoVerificacion.PENDIENTE)).thenReturn(lista);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/verificaciones?estado=PENDIENTE"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(lista))));
    }

    @Test
    void testListarConEstadoDesconocidoDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/verificaciones?estado=LOQUESEA"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
    }

    @Test
    void testAprobarDevuelveLaVerificacionResuelta() throws Exception {
        AdminVerificacionDTOResponse hecha = ejemplo(EstadoVerificacion.VERIFICADO);
        when(service.resolver(1L, true, null)).thenReturn(hecha);

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(hecha))));
    }

    @Test
    void testRechazarEnviaElMotivo() throws Exception {
        AdminVerificacionDTOResponse hecha = ejemplo(EstadoVerificacion.RECHAZADO);
        when(service.resolver(1L, false, "No existe")).thenReturn(hecha);

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":false,\"motivo\":\"No existe\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        verify(service).resolver(1L, false, "No existe");
    }

    @Test
    void testResolverSinAprobarDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"hola\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).resolver(anyLong(), anyBoolean(), any());
    }

    @Test
    void testResolverConMotivoMuyLargoDevuelve400() throws Exception {
        String largo = "a".repeat(201);

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":false,\"motivo\":\"" + largo + "\"}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).resolver(anyLong(), anyBoolean(), any());
    }

    @Test
    void testResolverYaRevisadaDevuelve400ConElMensaje() throws Exception {
        when(service.resolver(1L, true, null)).thenThrow(new VerificacionException("Esta verificación ya está revisada"));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true}"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), is(equalTo("Esta verificación ya está revisada")));
    }
}
