package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;
import dev.andrea.acompaname_backend.geolocation.dtos.DistanciaCuidadorDTOResponse;

@WebMvcTest(controllers = CercaniaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CercaniaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CercaniaService service;

    @Test
    void testDevuelveLasDistanciasDelLugarBuscado() throws Exception {
        when(service.buscarCerca("Oviedo")).thenReturn(new CercaniaDTOResponse(true,
                List.of(new DistanciaCuidadorDTOResponse(2L, 25.1), new DistanciaCuidadorDTOResponse(1L, 400.0))));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/cerca?lugar=Oviedo"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"lugarEncontrado\":true"));
        assertThat(response.getContentAsString(), containsString("\"cuidadorId\":2"));
        assertThat(response.getContentAsString(), containsString("\"km\":25.1"));
    }

    @Test
    void testUnLugarNoEncontradoDevuelve200ConLugarEncontradoFalso() throws Exception {
        when(service.buscarCerca("Xyz")).thenReturn(new CercaniaDTOResponse(false, List.of()));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/cerca?lugar=Xyz"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"lugarEncontrado\":false"));
    }

    @Test
    void testSinElParametroLugarNoFallaYPasaUnTextoVacio() throws Exception {
        when(service.buscarCerca("")).thenReturn(new CercaniaDTOResponse(false, List.of()));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/cerca"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        verify(service).buscarCerca("");
    }
}
