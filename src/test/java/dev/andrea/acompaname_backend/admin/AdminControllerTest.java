package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}