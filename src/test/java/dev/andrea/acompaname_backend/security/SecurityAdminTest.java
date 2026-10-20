package dev.andrea.acompaname_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    @Test
    @WithAnonymousUser
    void testSinIniciarSesionNoSePuedenVerLosUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "FAMILIA")
    void testUnaFamiliaNoPuedeVerLosUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUIDADOR")
    void testUnCuidadorNoPuedeVerLasSolicitudesDelAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/solicitudes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FAMILIA")
    void testUnaFamiliaNoPuedeBloquearUsuarios() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/usuarios/1/activo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void testElAdministradorVeLosUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void testElAdministradorVeLasSolicitudes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/solicitudes"))
                .andExpect(status().isOk());
    }
}