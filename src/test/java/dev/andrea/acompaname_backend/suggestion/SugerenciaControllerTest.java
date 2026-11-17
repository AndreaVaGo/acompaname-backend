package dev.andrea.acompaname_backend.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.security.DemasiadasPeticionesException;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SugerenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SugerenciaControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    private SugerenciaService service;
    @MockitoBean
    private LimitadorDePeticiones limitador;

    private MockHttpServletResponse enviar(String texto) throws Exception {
        String cuerpo = mapper.writeValueAsString(new SugerenciaDTORequest(texto));
        return mockMvc.perform(post("/api/v1/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testStoreDevuelve201ConLaSugerencia() throws Exception {
        SugerenciaDTOResponse creada = new SugerenciaDTOResponse(1L, "Más colores",
                LocalDateTime.of(2026, 11, 17, 22, 0));
        when(service.crear(ArgumentMatchers.any(SugerenciaDTORequest.class))).thenReturn(creada);

        MockHttpServletResponse response = enviar("Más colores");

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("Más colores"));
    }

    @Test
    void testStoreSinTextoDa400YNoGuarda() throws Exception {
        MockHttpServletResponse response = enviar("");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("no puede estar vacía"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreSoloConEspaciosDa400() throws Exception {
        MockHttpServletResponse response = enviar("     ");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreConMasDeMilCaracteresDa400() throws Exception {
        MockHttpServletResponse response = enviar("a".repeat(1001));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("no puede superar 1000"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testStoreConMilCaracteresEsValido() throws Exception {
        when(service.crear(ArgumentMatchers.any(SugerenciaDTORequest.class)))
                .thenReturn(new SugerenciaDTOResponse(1L, "a".repeat(1000), LocalDateTime.now()));

        MockHttpServletResponse response = enviar("a".repeat(1000));

        assertThat(response.getStatus(), is(equalTo(201)));
    }

    @Test
    void testStorePreguntaAlLimitadorConLaDireccionIP() throws Exception {
        when(service.crear(ArgumentMatchers.any(SugerenciaDTORequest.class)))
                .thenReturn(new SugerenciaDTOResponse(1L, "Hola", LocalDateTime.now()));

        enviar("Hola");

        // MockMvc hace las peticiones desde 127.0.0.1
        Mockito.verify(limitador).comprobar("sugerencia:127.0.0.1");
    }

    @Test
    void testSiHayDemasiadasPeticionesDa429YNoGuarda() throws Exception {
        Mockito.doThrow(new DemasiadasPeticionesException(
                "Demasiados intentos. Espera un minuto y vuelve a intentarlo."))
                .when(limitador).comprobar(ArgumentMatchers.anyString());

        MockHttpServletResponse response = enviar("Hola");

        assertThat(response.getStatus(), is(equalTo(429)));
        Mockito.verifyNoInteractions(service);
    }
}
