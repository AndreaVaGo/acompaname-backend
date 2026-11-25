package dev.andrea.acompaname_backend.geolocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import dev.andrea.acompaname_backend.security.DemasiadasPeticionesException;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;

@WebMvcTest(controllers = CercaniaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CercaniaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CercaniaService service;
    @MockitoBean
    private LimitadorDePeticiones limitador;

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

    @Test
    void testCadaBusquedaCuentaParaElLimiteDeSuDireccionIp() throws Exception {
        when(service.buscarCerca("Oviedo")).thenReturn(new CercaniaDTOResponse(false, List.of()));

        mockMvc.perform(get("/api/v1/cuidadores/cerca?lugar=Oviedo")).andReturn();

        // 127.0.0.1 es la dirección que MockMvc pone por defecto; 20 es el máximo por defecto
        verify(limitador).comprobar("cerca:127.0.0.1", 20);
    }

    @Test
    void testSiSeSuperaElLimiteDa429YNoSeBuscaNada() throws Exception {
        doThrow(new DemasiadasPeticionesException("Demasiados intentos. Espera un minuto y vuelve a intentarlo."))
                .when(limitador).comprobar(anyString(), anyInt());

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/cerca?lugar=Oviedo"))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(429)));
        verify(service, never()).buscarCerca(anyString());
    }
}
