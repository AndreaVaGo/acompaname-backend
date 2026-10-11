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
    private RoleService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndexDevuelveLosRoles() throws Exception {
        List<RoleDTOResponse> roles = List.of(
                new RoleDTOResponse(1L, "FAMILIA"),
                new RoleDTOResponse(2L, "CUIDADOR"));
        when(service.index()).thenReturn(roles);
        String json = mapper.writeValueAsString(roles);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/roles"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testIndexSinRolesDevuelveListaVacia() throws Exception {
        when(service.index()).thenReturn(List.of());

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/roles"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo("[]")));
    }
}