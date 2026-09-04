package dev.andrea.acompaname_backend.usuario;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UsuarioControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UsuarioService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndex() throws Exception {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Juan", "juan@test.com", "600111222", "1234", Rol.FAMILIA);
        List<UsuarioEntity> usuarios = new ArrayList<>();
        usuarios.add(usuario);
        String json = mapper.writeValueAsString(usuarios);
        when(service.getEntities()).thenReturn(usuarios);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Juan"));
    }
}
