package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.admin.dtos.AdminValoracionDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testEstadisticasDevuelveLosDatosDelPanel() throws Exception {
        EstadisticasDTOResponse estadisticas = new EstadisticasDTOResponse(10, 5, 3, 4, 6, 1, 2, 7, 6,
                new BigDecimal("120.00"), new BigDecimal("36.00"), 4.5, 2.0);
        when(service.estadisticas()).thenReturn(estadisticas);
        String json = mapper.writeValueAsString(estadisticas);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/estadisticas"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testAvisosDevuelveLosAvisosDelPanel() throws Exception {
        AvisosDTOResponse avisos = new AvisosDTOResponse(3, 1, 2);
        when(service.avisos()).thenReturn(avisos);
        String json = mapper.writeValueAsString(avisos);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/avisos"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testMejoresCuidadoresDevuelveLaLista() throws Exception {
        List<CuidadorValoradoDTOResponse> mejores = List.of(
                new CuidadorValoradoDTOResponse(1L, "Pepe", 4.8, 12),
                new CuidadorValoradoDTOResponse(2L, "Marta", 4.5, 7));
        when(service.mejoresCuidadores()).thenReturn(mejores);
        String json = mapper.writeValueAsString(mejores);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/cuidadores/mejor-valorados"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testUltimasValoracionesDevuelveLaLista() throws Exception {
        List<AdminValoracionDTOResponse> ultimas = List.of(
                new AdminValoracionDTOResponse(2L, 5, "Muy bien", LocalDate.of(2026, 11, 10), "Ana", "Pepe"),
                new AdminValoracionDTOResponse(1L, 3, "Normal", LocalDate.of(2026, 11, 9), "Luis", "Marta"));
        when(service.ultimasValoraciones()).thenReturn(ultimas);
        String json = mapper.writeValueAsString(ultimas);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/valoraciones/ultimas"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testBorrarValoracionDevuelve204() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/admin/valoraciones/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).borrarValoracion(1L);
    }
}