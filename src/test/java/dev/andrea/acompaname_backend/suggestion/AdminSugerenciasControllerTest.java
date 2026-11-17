package dev.andrea.acompaname_backend.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;

@WebMvcTest(controllers = AdminSugerenciasController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminSugerenciasControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SugerenciaService service;

    @Test
    void testIndexDevuelveLasSugerenciasConSuAutor() throws Exception {
        AdminSugerenciaDTOResponse sugerencia = new AdminSugerenciaDTOResponse(1L, "Más colores",
                LocalDateTime.of(2026, 11, 17, 22, 0), "Ana", "ana@test.com", "FAMILIA");
        when(service.listar()).thenReturn(List.of(sugerencia));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/sugerencias"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("Más colores"));
        assertThat(response.getContentAsString(), containsString("ana@test.com"));
        assertThat(response.getContentAsString(), containsString("FAMILIA"));
    }

    @Test
    void testIndexSinSugerenciasDevuelveUnaListaVacia() throws Exception {
        when(service.listar()).thenReturn(List.of());

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/admin/sugerencias"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo("[]")));
    }
}
