package dev.andrea.acompaname_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import tools.jackson.databind.ObjectMapper;

// Sin la configuración del proxy (que solo se activa en producción), la cabecera X-Forwarded-For
// no se tiene en cuenta: quien intente saltarse el límite cambiándola sigue contando como la misma IP.
@SpringBootTest(properties = { "rate-limit.max-requests=2", "rate-limit.window-seconds=30" })
@AutoConfigureMockMvc
public class SecurityLimiteCabeceraFalsaIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private int intentarLoginConIpFalsa(String ipFalsa) throws Exception {
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest("nadie.cabecera@test.com", "Secreta123"));
        return mockMvc.perform(post("/api/v1/login")
                .header("X-Forwarded-For", ipFalsa)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void testCambiarLaCabeceraNoPermiteSaltarseElLimite() throws Exception {
        assertEquals(401, intentarLoginConIpFalsa("1.1.1.1"));
        assertEquals(401, intentarLoginConIpFalsa("2.2.2.2"));

        // Aunque diga ser otra IP, sigue siendo la misma persona: se corta
        assertEquals(429, intentarLoginConIpFalsa("3.3.3.3"));
    }
}
