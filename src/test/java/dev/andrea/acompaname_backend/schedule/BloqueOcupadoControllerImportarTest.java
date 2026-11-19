package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

import dev.andrea.acompaname_backend.schedule.dtos.ImportacionCalendarioDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

@WebMvcTest(controllers = BloqueOcupadoController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BloqueOcupadoControllerImportarTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BloqueOcupadoService service;

    private MockHttpServletResponse importar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/bloques-ocupados/importar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testImportarDevuelve200ConElResumen() throws Exception {
        when(service.importar(ArgumentMatchers.anyString()))
                .thenReturn(new ImportacionCalendarioDTOResponse(3, 1, 2));

        MockHttpServletResponse response = importar("{\"contenido\":\"BEGIN:VCALENDAR\\r\\nEND:VCALENDAR\"}");

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"importados\":3"));
        assertThat(response.getContentAsString(), containsString("\"duplicados\":1"));
        assertThat(response.getContentAsString(), containsString("\"omitidos\":2"));
    }

    @Test
    void testImportarPasaElTextoDelArchivoAlServicio() throws Exception {
        when(service.importar(ArgumentMatchers.anyString()))
                .thenReturn(new ImportacionCalendarioDTOResponse(0, 0, 0));

        importar("{\"contenido\":\"BEGIN:VCALENDAR\\nEND:VCALENDAR\"}");

        Mockito.verify(service).importar("BEGIN:VCALENDAR\nEND:VCALENDAR");
    }

    @Test
    void testImportarSinContenidoDa400YNoLlamaAlServicio() throws Exception {
        MockHttpServletResponse response = importar("{}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testImportarConContenidoEnBlancoDa400() throws Exception {
        MockHttpServletResponse response = importar("{\"contenido\":\"   \"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testImportarConUnArchivoQueNoValeDa400() throws Exception {
        when(service.importar(ArgumentMatchers.anyString()))
                .thenThrow(new BloqueOcupadoExceptionFechaInvalida("El archivo no parece un calendario .ics válido"));

        MockHttpServletResponse response = importar("{\"contenido\":\"hola\"}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("calendario .ics"));
    }
}
