package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;

// Validación del recargo propio: debe estar entre 0 y 100
@WebMvcTest(controllers = PerfilCuidadorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilCuidadorControllerRecargoTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PerfilCuidadorService service;

    // JSON de un perfil válido con el recargo que le pasemos
    private String json(int recargo) {
        return "{\"especialidad\":\"Geriatría\",\"anosExperiencia\":4,\"tarifaHora\":18.00,\"bio\":\"Bio\","
                + "\"tieneVehiculo\":true,\"disponibleAhora\":true,\"usuarioId\":2,\"recargoPersonaExtra\":"
                + recargo + "}";
    }

    @Test
    void testStoreConRecargoMayorQueCienDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(json(101)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(PerfilCuidadorDTORequest.class));
    }

    @Test
    void testStoreConRecargoNegativoDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(json(-1)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(PerfilCuidadorDTORequest.class));
    }

    @Test
    void testStoreConRecargoValidoNoDa400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(json(30)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus() != 400, is(true));
    }
}
