package dev.andrea.acompaname_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void testSinIniciarSesionNoSePuedeEntrarEnAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/estadisticas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "FAMILIA")
    void testUnaFamiliaNoPuedeEntrarEnAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/estadisticas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUIDADOR")
    void testUnCuidadorNoPuedeEntrarEnAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/estadisticas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void testElAdministradorVeLasEstadisticas() throws Exception {
        mockMvc.perform(get("/api/v1/admin/estadisticas"))
                .andExpect(status().isOk());
    }
}