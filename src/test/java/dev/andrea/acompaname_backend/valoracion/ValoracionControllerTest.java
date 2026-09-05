package dev.andrea.acompaname_backend.valoracion;

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

import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ValoracionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ValoracionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ValoracionService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndex() throws Exception {
        ValoracionDTOResponse valoracion = new ValoracionDTOResponse(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), 1L);
        List<ValoracionDTOResponse> valoraciones = new ArrayList<>();
        valoraciones.add(valoracion);
        String json = mapper.writeValueAsString(valoraciones);
        when(service.getEntities()).thenReturn(valoraciones);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/valoraciones"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Muy buena atención"));
    }

    @Test
    void testGetById() throws Exception {
        ValoracionDTOResponse valoracion = new ValoracionDTOResponse(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), 1L);
        String json = mapper.writeValueAsString(valoracion);
        when(service.getById(1L)).thenReturn(valoracion);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/valoraciones/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Muy buena atención"));
    }

    @Test
    void testStore() throws Exception {
        ValoracionDTORequest dto = new ValoracionDTORequest("Excelente trato", 5, LocalDate.of(2026, 9, 12), 1L);
        ValoracionDTOResponse dtoResponse = new ValoracionDTOResponse(1L, "Excelente trato", 5,
                LocalDate.of(2026, 9, 12), 1L);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(ValoracionDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/valoraciones")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Excelente trato"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/valoraciones/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }
}