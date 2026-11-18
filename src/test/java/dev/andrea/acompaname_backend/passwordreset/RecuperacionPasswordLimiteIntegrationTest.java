package dev.andrea.acompaname_backend.passwordreset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Con un límite de 2 intentos, el tercero de la misma IP recibe 429 en los dos endpoints nuevos
@SpringBootTest(properties = { "rate-limit.max-requests=2", "rate-limit.window-seconds=30" })
@AutoConfigureMockMvc
public class RecuperacionPasswordLimiteIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;

    private int enviar(String ruta, String cuerpo) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/" + ruta)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)).andReturn().getResponse().getStatus();
    }

    @Test
    void testOlvidePasswordSeLimitaAlTercerIntento() throws Exception {
        String cuerpo = "{\"email\":\"nadie.limite@test.com\"}";

        assertEquals(202, enviar("olvide-password", cuerpo));
        assertEquals(202, enviar("olvide-password", cuerpo));
        assertEquals(429, enviar("olvide-password", cuerpo));
    }

    @Test
    void testRestablecerPasswordSeLimitaAlTercerIntento() throws Exception {
        String cuerpo = "{\"token\":\"falso\",\"passwordNueva\":\"Nueva1234!\"}";

        assertEquals(400, enviar("restablecer-password", cuerpo));
        assertEquals(400, enviar("restablecer-password", cuerpo));
        assertEquals(429, enviar("restablecer-password", cuerpo));
    }
}
