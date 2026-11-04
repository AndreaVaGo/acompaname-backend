package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PagoController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PagoControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PagoService service;
    @Autowired
    ObjectMapper mapper;

    private PagoDTOResponse crearPagoResponse(EstadoPago estado) {
        return new PagoDTOResponse(1L, new BigDecimal("90.00"), estado, LocalDate.of(2026, 9, 11), 1L);
    }

    @Test
    void testIndex() throws Exception {
        List<PagoDTOResponse> pagos = new ArrayList<>();
        pagos.add(crearPagoResponse(EstadoPago.PENDIENTE));
        String json = mapper.writeValueAsString(pagos);
        when(service.getEntities()).thenReturn(pagos);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("PENDIENTE"));
    }

    @Test
    void testGetById() throws Exception {
        PagoDTOResponse pago = crearPagoResponse(EstadoPago.PENDIENTE);
        String json = mapper.writeValueAsString(pago);
        when(service.getById(1L)).thenReturn(pago);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/pagos/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testGetByIdNoExiste() throws Exception {
        when(service.getById(99L)).thenThrow(new PagoExceptionNotFound("Pago no encontrado. Id 99 no existe."));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/pagos/99"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(404)));
    }

    // El pago se crea solo al completar la solicitud: la familia no puede crearlo a mano
    @Test
    void testNoSePuedeCrearUnPagoDesdeLaApi() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/pagos")
                .contentType(MediaType.APPLICATION_JSON).content("{\"importe\":0.01,\"solicitudId\":1}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(405)));
        Mockito.verifyNoInteractions(service);
    }

    // El importe sale de la tarifa del cuidador: la familia no puede cambiarlo
    @Test
    void testNoSePuedeCambiarElImporteDeUnPagoDesdeLaApi() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/pagos/1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"importe\":0.01,\"solicitudId\":1}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(405)));
        Mockito.verifyNoInteractions(service);
    }

    // Si la familia pudiera borrar su pago pendiente, se libraría de pagar un servicio ya hecho
    @Test
    void testNoSePuedeBorrarUnPagoDesdeLaApi() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/pagos/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(405)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testMarcarComoPagado() throws Exception {
        PagoDTOResponse dtoResponse = crearPagoResponse(EstadoPago.COMPLETADO);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.marcarComoPagado(1L)).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/pagos/1/pagar"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("COMPLETADO"));
    }

    @Test
    void testMarcarComoPagadoNoExiste() throws Exception {
        when(service.marcarComoPagado(99L))
                .thenThrow(new PagoExceptionNotFound("Pago no encontrado. Id 99 no existe."));

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/pagos/99/pagar"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(404)));
    }

    @Test
    void testMarcarComoPagadoSinPermiso() throws Exception {
        when(service.marcarComoPagado(1L))
                .thenThrow(new PagoExceptionAccesoDenegado("Solo la familia de la solicitud puede pagar"));

        MockHttpServletResponse response = mockMvc.perform(patch("/api/v1/pagos/1/pagar"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(403)));
    }
}