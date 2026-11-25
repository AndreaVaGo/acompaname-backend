package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// La aplicación completa con MySQL de verdad: el hosting puede comprobar la salud sin iniciar sesión,
// y eso es lo único de actuator que está abierto.
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false" })
@AutoConfigureMockMvc
public class SaludIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletResponse pedir(String ruta) throws Exception {
        return mockMvc.perform(get(ruta)).andReturn().getResponse();
    }

    @Test
    void testLaSaludSePuedeConsultarSinIniciarSesion() throws Exception {
        MockHttpServletResponse response = pedir("/actuator/health");

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"status\":\"UP\""));
    }

    @Test
    void testLaSaludNoEnseñaDetalles() throws Exception {
        String cuerpo = pedir("/actuator/health").getContentAsString();

        assertThat(cuerpo, is(not(containsString("components"))));
        assertThat(cuerpo, is(not(containsString("db"))));
        assertThat(cuerpo, is(not(containsString("MySQL"))));
        assertThat(cuerpo, is(not(containsString("diskSpace"))));
    }

    @Test
    void testLaComprobacionDeVidaYDeDisponibilidadFuncionan() throws Exception {
        MockHttpServletResponse vida = pedir("/actuator/health/liveness");
        MockHttpServletResponse disponible = pedir("/actuator/health/readiness");

        assertThat(vida.getStatus(), is(equalTo(200)));
        assertThat(vida.getContentAsString(), containsString("\"status\":\"UP\""));
        assertThat(disponible.getStatus(), is(equalTo(200)));
        assertThat(disponible.getContentAsString(), containsString("\"status\":\"UP\""));
    }

    @Test
    void testElRestoDeActuatorNoSeExponeNiSinSesion() throws Exception {
        assertThat(pedir("/actuator/env").getStatus(), is(equalTo(401)));
        assertThat(pedir("/actuator/beans").getStatus(), is(equalTo(401)));
        assertThat(pedir("/actuator/info").getStatus(), is(equalTo(401)));
        assertThat(pedir("/actuator/heapdump").getStatus(), is(equalTo(401)));
    }

    @Test
    void testSoloSePuedeConsultarLaSaludNoCambiarla() throws Exception {
        int estado = mockMvc.perform(post("/actuator/health")).andReturn().getResponse().getStatus();

        assertThat(estado, is(equalTo(401)));
    }
}
