package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;

// El endpoint que envía la foto como imagen
@WebMvcTest(controllers = PerfilCuidadorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilCuidadorControllerFotoTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PerfilCuidadorService service;

    @Test
    void testEnviaLosBytesDeLaImagenConSuTipo() throws Exception {
        byte[] bytes = { 1, 2, 3, 4 };
        when(service.getFoto(5L)).thenReturn(new FotoDescargable("image/png", bytes));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/5/foto"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getContentType(), is(equalTo("image/png")));
        assertThat(response.getContentAsByteArray(), is(equalTo(bytes)));
    }

    @Test
    void testElNavegadorPuedeGuardarLaFotoUnDiaEnCache() throws Exception {
        when(service.getFoto(5L)).thenReturn(new FotoDescargable("image/jpeg", new byte[] { 1 }));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/5/foto")).andReturn()
                .getResponse();

        assertThat(response.getHeader("Cache-Control"), containsString("max-age=86400"));
        assertThat(response.getHeader("Cache-Control"), containsString("public"));
    }

    @Test
    void testSinFotoResponde404() throws Exception {
        when(service.getFoto(5L)).thenThrow(new PerfilCuidadorExceptionNotFound("Este perfil no tiene foto."));

        mockMvc.perform(get("/api/v1/cuidadores/5/foto")).andExpect(status().isNotFound());
    }
}
