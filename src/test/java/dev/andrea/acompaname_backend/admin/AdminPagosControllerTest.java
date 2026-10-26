package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.admin.dtos.AdminPagoCuidadorDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoMesDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagoTipoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminPagosResumenDTOResponse;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminPagosController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminPagosControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminPagosService service;
    @Autowired
    ObjectMapper mapper;

    private AdminPagoDTOResponse fila() {
        return new AdminPagoDTOResponse(1L, 5L, "Ana", "Pepe", "domicilio", new BigDecimal("36.00"),
                EstadoPago.PENDIENTE, LocalDate.of(2026, 10, 20), 6);
    }

    @Test
    void testResumenDevuelveElAnalisisDeLosPagos() throws Exception {
        AdminPagosResumenDTOResponse resumen = new AdminPagosResumenDTOResponse(new BigDecimal("54.00"),
                new BigDecimal("36.00"), 2, 1, 0, new BigDecimal("30.00"), 66.7, 1, new BigDecimal("36.00"),
                List.of(new AdminPagoTipoDTOResponse("domicilio", 3, new BigDecimal("90.00"))),
                List.of(new AdminPagoMesDTOResponse("2026-10", new BigDecimal("54.00"), new BigDecimal("36.00"))),
                List.of(new AdminPagoCuidadorDTOResponse(1L, "Pepe", 2, new BigDecimal("54.00"))));
        when(service.resumen()).thenReturn(resumen);
        String json = mapper.writeValueAsString(resumen);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/pagos/resumen"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testPagosSinEstadoDevuelveTodos() throws Exception {
        List<AdminPagoDTOResponse> pagos = List.of(fila());
        when(service.pagos(null)).thenReturn(pagos);
        String json = mapper.writeValueAsString(pagos);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/pagos"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        verify(service).pagos(null);
    }

    @Test
    void testPagosConEstadoPasaElFiltroAlServicio() throws Exception {
        when(service.pagos(EstadoPago.PENDIENTE)).thenReturn(List.of(fila()));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/pagos").param("estado", "PENDIENTE"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        verify(service).pagos(EstadoPago.PENDIENTE);
    }

    @Test
    void testPagosConUnEstadoInventadoDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/pagos").param("estado", "INVENTADO"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verifyNoInteractions(service);
    }
}