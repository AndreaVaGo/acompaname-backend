package dev.andrea.acompaname_backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void testSinLoginDa401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@test.com", authorities = { "FAMILIA" })
    void testConLoginDa200() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testRegistroEsPublico() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "familia@test.com", authorities = { "FAMILIA" })
    void testFamiliaNoPuedeCrearCuidador() throws Exception {
        mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void testListadoDeCuidadoresEsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testPerfilDeCuidadorEsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void testMiPerfilDeCuidadorNoEsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/mi-perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void testSolicitudesSinLoginDa401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes/mis-solicitudes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void testPagosSinLoginDa401() throws Exception {
        mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void testCrearSolicitudSinLoginDa401() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}