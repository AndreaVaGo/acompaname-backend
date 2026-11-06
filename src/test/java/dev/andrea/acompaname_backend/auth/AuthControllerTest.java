package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtService jwtService;

    // Hace que el AuthenticationManager acepte el login y devuelva a esa persona
    private void simularLoginCorrecto(Long id, String email, String nombreRol) {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName(nombreRol);
        UsuarioEntity usuario = new UsuarioEntity(id, "Ana", email, "600111222", "1234", Set.of(rol));
        SecurityUser securityUser = new SecurityUser(usuario);
        Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null,
                securityUser.getAuthorities());
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenReturn(auth);
        Mockito.when(jwtService.crearToken(usuario)).thenReturn("token-de-prueba");
    }

    private MockHttpServletResponse hacerLogin(String email, String password) throws Exception {
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest(email, password));
        return mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testLoginDevuelveIdEmailRolYToken() throws Exception {
        simularLoginCorrecto(5L, "ana@test.com", "FAMILIA");
        String json = mapper.writeValueAsString(
                new AuthDTOResponse(5L, "Logged", "ana@test.com", "FAMILIA", "token-de-prueba"));

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
    }

    @Test
    void testLoginDeUnCuidadorDevuelveSuRol() throws Exception {
        simularLoginCorrecto(8L, "pepe@test.com", "CUIDADOR");

        MockHttpServletResponse response = hacerLogin("pepe@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("CUIDADOR"));
        assertThat(response.getContentAsString(), containsString("pepe@test.com"));
    }

    @Test
    void testConEmailOContrasenaIncorrectosDa401SinToken() throws Exception {
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        MockHttpServletResponse response = hacerLogin("ana@test.com", "mala");

        assertThat(response.getStatus(), is(equalTo(401)));
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testUnaPersonaBloqueadaDa401SinToken() throws Exception {
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new DisabledException("Usuario bloqueado"));

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(401)));
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testSinEmailDa400YNoSeIntentaElLogin() throws Exception {
        MockHttpServletResponse response = hacerLogin("", "1234");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(authenticationManager);
    }

    @Test
    void testSinContrasenaDa400YNoSeIntentaElLogin() throws Exception {
        MockHttpServletResponse response = hacerLogin("ana@test.com", "");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(authenticationManager);
    }

    // El login con GET (la forma antigua con Basic) ya no existe
    @Test
    void testElLoginConGetYaNoEstaPermitido() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/login"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(405)));
    }
}