package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

import dev.andrea.acompaname_backend.captcha.CaptchaInvalidoException;
import dev.andrea.acompaname_backend.captcha.VerificadorCaptcha;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.security.DemasiadasPeticionesException;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(OutputCaptureExtension.class)
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
    @MockitoBean
    private LimitadorDePeticiones limitador;
    @MockitoBean
    private VerificadorCaptcha verificadorCaptcha;
    @MockitoBean
    private DosPasosService dosPasosService;

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
    void testUnLoginFallidoDejaUnAvisoConLaIPYSinDatosPersonales(CapturedOutput salida) throws Exception {
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        hacerLogin("ana@test.com", "clave-secreta");

        // MockMvc hace las peticiones desde 127.0.0.1
        assertThat(salida.getOut(), containsString("Intento de login fallido desde la IP 127.0.0.1"));
        assertThat(salida.getOut(), not(containsString("ana@test.com")));
        assertThat(salida.getOut(), not(containsString("clave-secreta")));
    }

    @Test
    void testUnLoginCorrectoNoDejaAvisoDeFallo(CapturedOutput salida) throws Exception {
        simularLoginCorrecto(5L, "ana@test.com", "FAMILIA");

        hacerLogin("ana@test.com", "1234");

        assertThat(salida.getOut(), not(containsString("Intento de login fallido")));
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

    // ---------- Límite de peticiones ----------

    @Test
    void testElLoginPreguntaAlLimitadorConLaDireccionIP() throws Exception {
        simularLoginCorrecto(5L, "ana@test.com", "FAMILIA");

        hacerLogin("ana@test.com", "1234");

        // MockMvc hace las peticiones desde 127.0.0.1
        Mockito.verify(limitador).comprobar("login:127.0.0.1");
    }

    @Test
    void testSiHayDemasiadosIntentosDa429YNoSeComprueba() throws Exception {
        Mockito.doThrow(new DemasiadasPeticionesException(
                "Demasiados intentos. Espera un minuto y vuelve a intentarlo."))
                .when(limitador).comprobar(ArgumentMatchers.anyString());

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(429)));
        assertThat(response.getContentAsString(), containsString("Demasiados intentos"));
        Mockito.verifyNoInteractions(authenticationManager);
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testUnaPeticionMalEscritaNoCuentaParaElLimite() throws Exception {
        hacerLogin("", "1234");

        Mockito.verifyNoInteractions(limitador);
    }

    @Test
    void testElLoginMandaElTokenDelCaptchaYLaIpAlVerificador() throws Exception {
        simularLoginCorrecto(5L, "ana@test.com", "FAMILIA");
        String cuerpo = mapper.writeValueAsString(new LoginDTORequest("ana@test.com", "1234", "token-captcha"));

        mockMvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse();

        // MockMvc hace las peticiones desde 127.0.0.1
        Mockito.verify(verificadorCaptcha).verificar("token-captcha", "127.0.0.1");
    }

    @Test
    void testSiElCaptchaNoEsValidoDa400YNoSeComprueba() throws Exception {
        Mockito.doThrow(new CaptchaInvalidoException("El CAPTCHA no es válido. Inténtalo de nuevo."))
                .when(verificadorCaptcha).verificar(ArgumentMatchers.any(), ArgumentMatchers.anyString());

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("CAPTCHA"));
        Mockito.verifyNoInteractions(authenticationManager);
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testElCaptchaSeMiraDespuesDelLimite() throws Exception {
        Mockito.doThrow(new DemasiadasPeticionesException("Demasiados intentos."))
                .when(limitador).comprobar(ArgumentMatchers.anyString());

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(429)));
        Mockito.verifyNoInteractions(verificadorCaptcha);
    }

    @Test
    void testUnLoginMalEscritoNoLlegaAlCaptcha() throws Exception {
        hacerLogin("", "1234");

        Mockito.verifyNoInteractions(verificadorCaptcha);
    }

    // ---------- Inicio de sesión en dos pasos ----------

    // Hace que el login sea correcto con una persona que tiene el segundo paso activado
    private UsuarioEntity simularLoginConDosPasos() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        UsuarioEntity usuario = new UsuarioEntity(5L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol));
        usuario.setDosPasos(true);
        SecurityUser securityUser = new SecurityUser(usuario);
        Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null,
                securityUser.getAuthorities());
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenReturn(auth);
        return usuario;
    }

    private MockHttpServletResponse hacerVerificacion(String desafio, String codigo) throws Exception {
        String cuerpo = mapper.writeValueAsString(new VerificarCodigoDTORequest(desafio, codigo));
        return mockMvc.perform(post("/api/v1/login/verificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andReturn()
                .getResponse();
    }

    @Test
    void testConDosPasosElLoginDa202ConElDesafioYSinToken() throws Exception {
        UsuarioEntity usuario = simularLoginConDosPasos();
        Mockito.when(dosPasosService.iniciar(usuario)).thenReturn("desafio-de-prueba");

        MockHttpServletResponse response = hacerLogin("ana@test.com", "1234");

        assertThat(response.getStatus(), is(equalTo(202)));
        assertThat(response.getContentAsString(),
                is(equalTo(mapper.writeValueAsString(new DosPasosDTOResponse(true, "desafio-de-prueba")))));
        assertThat(response.getContentAsString(), not(containsString("token")));
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testSinDosPasosElLoginNoMandaNingunCodigo() throws Exception {
        simularLoginCorrecto(5L, "ana@test.com", "FAMILIA");

        hacerLogin("ana@test.com", "1234");

        Mockito.verifyNoInteractions(dosPasosService);
    }

    @Test
    void testSiFallaLaContrasenaNoSeMandaNingunCodigo() throws Exception {
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        hacerLogin("ana@test.com", "mala");

        Mockito.verifyNoInteractions(dosPasosService);
    }

    @Test
    void testVerificarConCodigoCorrectoDevuelveElToken() throws Exception {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("CUIDADOR");
        UsuarioEntity usuario = new UsuarioEntity(8L, "Pepe", "pepe@test.com", "600111222", "1234", Set.of(rol));
        Mockito.when(dosPasosService.verificar("desafio", "123456")).thenReturn(usuario);
        Mockito.when(jwtService.crearToken(usuario)).thenReturn("token-de-prueba");

        MockHttpServletResponse response = hacerVerificacion("desafio", "123456");

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(
                new AuthDTOResponse(8L, "Logged", "pepe@test.com", "CUIDADOR", "token-de-prueba")))));
    }

    @Test
    void testVerificarConCodigoIncorrectoDa400SinToken() throws Exception {
        Mockito.when(dosPasosService.verificar("desafio", "000000")).thenThrow(new CodigoInvalidoException());

        MockHttpServletResponse response = hacerVerificacion("desafio", "000000");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("El código no es válido"));
        Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void testVerificarSinCodigoDa400YNoSeComprueba() throws Exception {
        MockHttpServletResponse response = hacerVerificacion("desafio", "");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(dosPasosService);
    }

    @Test
    void testVerificarSinDesafioDa400YNoSeComprueba() throws Exception {
        MockHttpServletResponse response = hacerVerificacion("", "123456");

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(dosPasosService);
    }

    @Test
    void testVerificarPreguntaAlLimitadorConLaDireccionIP() throws Exception {
        Mockito.when(dosPasosService.verificar("desafio", "123456")).thenThrow(new CodigoInvalidoException());

        hacerVerificacion("desafio", "123456");

        Mockito.verify(limitador).comprobar("verificar:127.0.0.1");
    }

    @Test
    void testVerificarConDemasiadosIntentosDa429YNoSeComprueba() throws Exception {
        Mockito.doThrow(new DemasiadasPeticionesException(
                "Demasiados intentos. Espera un minuto y vuelve a intentarlo."))
                .when(limitador).comprobar(ArgumentMatchers.anyString());

        MockHttpServletResponse response = hacerVerificacion("desafio", "123456");

        assertThat(response.getStatus(), is(equalTo(429)));
        Mockito.verifyNoInteractions(dosPasosService);
    }
}
