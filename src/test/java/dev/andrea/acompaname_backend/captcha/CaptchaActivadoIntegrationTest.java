package dev.andrea.acompaname_backend.captcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;
import tools.jackson.databind.ObjectMapper;

// La aplicación completa con el CAPTCHA activado. Sin token nunca se llega a preguntar a Cloudflare,
// así que estos tests no necesitan internet.
@SpringBootTest(properties = { "captcha.enabled=true", "captcha.secret=clave-de-prueba",
        "rate-limit.max-requests=1000" })
@AutoConfigureMockMvc
public class CaptchaActivadoIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private VerificadorCaptcha verificador;

    @Test
    void testConElCaptchaActivadoSeUsaTurnstile() {
        assertInstanceOf(VerificadorTurnstile.class, verificador);
    }

    @Test
    void testUnLoginSinTokenDa400() throws Exception {
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest("nadie@test.com", "Secreta123"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("CAPTCHA"));
    }

    @Test
    void testUnLoginConTokenVacioDa400() throws Exception {
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest("nadie@test.com", "Secreta123", ""));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
    }

    @Test
    void testUnRegistroSinTokenDa400() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana-captcha@test.com", "600555666",
                "Segura123!", Set.of(1L));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("CAPTCHA"));
    }
}
