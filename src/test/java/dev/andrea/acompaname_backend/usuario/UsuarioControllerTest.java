package dev.andrea.acompaname_backend.usuario;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
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
        UsuarioDTOResponse usuario = new UsuarioDTOResponse(1L, "Juan", "juan@test.com", "600111222",
                Set.of("FAMILIA"));
        List<UsuarioDTOResponse> usuarios = new ArrayList<>();
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

    @Test
    void testGetById() throws Exception {
        UsuarioDTOResponse usuario = new UsuarioDTOResponse(1L, "Juan", "juan@test.com", "600111222",
                Set.of("FAMILIA"));
        String json = mapper.writeValueAsString(usuario);
        when(service.getById(1L)).thenReturn(usuario);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/usuarios/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Juan"));
    }

    @Test
    void testStore() throws Exception {
        UsuarioDTORequest dto = new UsuarioDTORequest("Ana", "ana@test.com", "600555666", "12345678", Set.of(1L));
        UsuarioDTOResponse dtoResponse = new UsuarioDTOResponse(1L, "Ana", "ana@test.com", "600555666",
                Set.of("FAMILIA"));
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(UsuarioDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Ana"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/usuarios/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }
}