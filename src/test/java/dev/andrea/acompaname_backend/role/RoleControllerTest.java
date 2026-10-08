package dev.andrea.acompaname_backend.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.dtos.RoleDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RoleControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RoleRepository repository;
    @Autowired
    ObjectMapper mapper;

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    @Test
    void testIndexDevuelveLosRoles() throws Exception {
        when(repository.findAll()).thenReturn(List.of(rol(1L, "FAMILIA"), rol(2L, "CUIDADOR")));
        String json = mapper.writeValueAsString(
                List.of(new RoleDTOResponse(1L, "FAMILIA"), new RoleDTOResponse(2L, "CUIDADOR")));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/roles"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testIndexSinRolesDevuelveListaVacia() throws Exception {
        when(repository.findAll()).thenReturn(List.of());

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/roles"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo("[]")));
    }
}