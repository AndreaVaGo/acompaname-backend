package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(Long id, String email, String nombreRol) {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName(nombreRol);
        UsuarioEntity usuario = new UsuarioEntity(id, "Ana", email, "600111222", "1234", Set.of(rol));
        SecurityUser securityUser = new SecurityUser(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
    }

    @Test
    void testLoginDevuelveIdEmailYRolDelUsuarioLogueado() throws Exception {
        loguearComo(5L, "ana@test.com", "FAMILIA");
        String json = mapper.writeValueAsString(new AuthDTOResponse(5L, "Logged", "ana@test.com", "FAMILIA"));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/login"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testLoginDeUnCuidadorDevuelveSuRol() throws Exception {
        loguearComo(8L, "pepe@test.com", "CUIDADOR");

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/login"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("CUIDADOR"));
        assertThat(response.getContentAsString(), containsString("pepe@test.com"));
    }
}