package dev.andrea.acompaname_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityValoracionesPublicasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void testValoracionesDeUnCuidadorSonPublicas() throws Exception {
        mockMvc.perform(get("/api/v1/valoraciones/cuidador/999999"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testListadoCompletoDeValoracionesSigueSiendoPrivado() throws Exception {
        mockMvc.perform(get("/api/v1/valoraciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void testUnaValoracionConcretaSigueSiendoPrivada() throws Exception {
        mockMvc.perform(get("/api/v1/valoraciones/1"))
                .andExpect(status().isUnauthorized());
    }
}