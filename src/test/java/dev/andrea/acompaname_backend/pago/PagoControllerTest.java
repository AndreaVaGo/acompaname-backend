package dev.andrea.acompaname_backend.pago;

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

import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.pago.exceptions.PagoExceptionNotFound;
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

    @Test
    void testStore() throws Exception {
        PagoDTORequest dto = new PagoDTORequest(new BigDecimal("90.00"), 1L);
        PagoDTOResponse dtoResponse = crearPagoResponse(EstadoPago.PENDIENTE);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(PagoDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/pagos")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testStoreSinImporte() throws Exception {
        PagoDTORequest dto = new PagoDTORequest(null, 1L);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/pagos")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("El importe no puede ser nulo"));
    }

    @Test
    void testStoreSinSolicitud() throws Exception {
        PagoDTORequest dto = new PagoDTORequest(new BigDecimal("90.00"), null);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/pagos")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La solicitud no puede ser nula"));
    }

    @Test
    void testUpdate() throws Exception {
        PagoDTORequest dto = new PagoDTORequest(new BigDecimal("120.00"), 1L);
        PagoDTOResponse dtoResponse = new PagoDTOResponse(1L, new BigDecimal("120.00"), EstadoPago.PENDIENTE,
                LocalDate.of(2026, 9, 11), 1L);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.update(Mockito.eq(1L), Mockito.any(PagoDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/pagos/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/pagos/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
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
}