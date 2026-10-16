package dev.andrea.acompaname_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityCorsTest {

    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";
    private static final String ORIGEN_AJENO = "http://web-que-no-es-la-nuestra.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPeticionPreviaDesdeElFrontEsPermitida() throws Exception {
        mockMvc.perform(options("/api/v1/solicitudes")
                .header("Origin", ORIGEN_PERMITIDO)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO));
    }

    @Test
    void testPeticionPreviaDesdeOtroOrigenSeRechaza() throws Exception {
        mockMvc.perform(options("/api/v1/solicitudes")
                .header("Origin", ORIGEN_AJENO)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPeticionNormalDesdeElFrontLlevaLaCabeceraDeCors() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                .header("Origin", ORIGEN_PERMITIDO))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_PERMITIDO));
    }
}