package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// La aplicación completa con los mismos ajustes de errores que el perfil de producción:
// ninguna respuesta de error debe enseñar trazas, nombres de clases ni mensajes internos.
@SpringBootTest(properties = {
        "server.error.include-message=never",
        "server.error.include-stacktrace=never",
        "server.error.include-binding-errors=never",
        "server.error.include-exception=false",
        "server.error.whitelabel.enabled=false",
        "rate-limit.max-requests=1000" })
@AutoConfigureMockMvc
public class ErroresProduccionIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;

    // Un texto que salga en una respuesta de error nunca debe parecerse a una traza de Java
    private void comprobarQueNoFiltraDatosInternos(MockHttpServletResponse respuesta) throws Exception {
        String cuerpo = respuesta.getContentAsString();

        assertThat(cuerpo, is(not(containsString("Exception"))));
        assertThat(cuerpo, is(not(containsString("stacktrace"))));
        assertThat(cuerpo, is(not(containsString("trace"))));
        assertThat(cuerpo, is(not(containsString("dev.andrea"))));
        assertThat(cuerpo, is(not(containsString("org.springframework"))));
        assertThat(cuerpo, is(not(containsString(".java"))));
        assertThat(cuerpo, is(not(containsString("Whitelabel"))));
    }

    @Test
    void testUnJsonRotoNoFiltraDatosInternos() throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ esto no es json"))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus(), is(400));
        comprobarQueNoFiltraDatosInternos(respuesta);
    }

    @Test
    void testUnaRutaQueNoExisteNoFiltraDatosInternos() throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(get("/api/v1/ruta-que-no-existe"))
                .andReturn().getResponse();

        comprobarQueNoFiltraDatosInternos(respuesta);
    }

    @Test
    void testUnMetodoNoPermitidoNoFiltraDatosInternos() throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(get("/api/v1/login"))
                .andReturn().getResponse();

        comprobarQueNoFiltraDatosInternos(respuesta);
    }

    @Test
    void testUnIdQueNoEsUnNumeroNoFiltraDatosInternos() throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(get("/api/v1/cuidadores/abc"))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus(), is(400));
        comprobarQueNoFiltraDatosInternos(respuesta);
    }

    @Test
    void testUnaValidacionFallidaNoFiltraDatosInternos() throws Exception {
        MockHttpServletResponse respuesta = mockMvc.perform(post("/api/v1/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\",\"passwordNueva\":\"123\"}"))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus(), is(400));
        comprobarQueNoFiltraDatosInternos(respuesta);
    }
}
