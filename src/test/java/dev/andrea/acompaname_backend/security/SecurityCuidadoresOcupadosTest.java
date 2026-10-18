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
public class SecurityCuidadoresOcupadosTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void testLosCuidadoresOcupadosSeConsultanSinIniciarSesion() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/cuidadores-ocupados").param("inicio", "2026-09-10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testElListadoDeSolicitudesSigueSiendoPrivado() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes"))
                .andExpect(status().isUnauthorized());
    }
}