package dev.andrea.acompaname_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import tools.jackson.databind.ObjectMapper;

// La aplicación completa con un MySQL de verdad. Se baja el límite a 2 intentos cada
// 2 segundos para poder comprobarlo sin esperar un minuto entero.
@SpringBootTest(properties = { "rate-limit.max-requests=2", "rate-limit.window-seconds=2" })
@AutoConfigureMockMvc
@Testcontainers
public class SecurityLimiteIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    // Un login con una cuenta que no existe: siempre falla con 401, pero cuenta como intento
    private MockHttpServletResponse intentarLogin() throws Exception {
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest("nadie@test.com", "Secreta123"));
        return mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testTrasSuperarElLimiteDa429YDespuesDeEsperarVuelveAPasar() throws Exception {
        // Los dos primeros intentos llegan hasta la comprobación (y fallan con 401)
        assertEquals(401, intentarLogin().getStatus());
        assertEquals(401, intentarLogin().getStatus());

        // El tercero se para antes de comprobar nada
        MockHttpServletResponse bloqueado = intentarLogin();
        assertEquals(429, bloqueado.getStatus());
        assertTrue(bloqueado.getContentAsString().contains("Demasiados intentos"));

        // Pasada la ventana de 2 segundos, se puede volver a intentar
        Thread.sleep(2500);
        assertEquals(401, intentarLogin().getStatus());
    }
}