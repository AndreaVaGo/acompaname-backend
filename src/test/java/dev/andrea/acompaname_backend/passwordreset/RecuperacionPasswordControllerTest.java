package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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

import dev.andrea.acompaname_backend.security.DemasiadasPeticionesException;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionPasswordInvalida;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = RecuperacionPasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RecuperacionPasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private RecuperacionPasswordService service;
    @MockitoBean
    private LimitadorDePeticiones limitador;

    private MockHttpServletResponse pedirEnlace(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/olvide-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new OlvidePasswordDTORequest(email))))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse restablecer(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RestablecerPasswordDTORequest(token, password))))
                .andReturn().getResponse();
    }

    // ---------- /olvide-password ----------

    @Test
    void testOlvidePasswordDevuelve202YPasaElEmailAlServicio() throws Exception {
        MockHttpServletResponse respuesta = pedirEnlace("ana@test.com");

        assertThat(respuesta.getStatus(), is(202));
        assertThat(respuesta.getContentAsString(), is(equalTo(RecuperacionPasswordController.MENSAJE_OLVIDE)));
        verify(service).solicitarRestablecimiento("ana@test.com");
    }

    @Test
    void testOlvidePasswordCompruebaElLimiteDePeticiones() throws Exception {
        pedirEnlace("ana@test.com");

        verify(limitador).comprobar(org.mockito.ArgumentMatchers.startsWith("olvide:"));
    }

    @Test
    void testOlvidePasswordContestaLoMismoSiElCorreoNoExiste() throws Exception {
        // El servicio no avisa de nada cuando el correo no existe, así que la respuesta es idéntica
        MockHttpServletResponse existe = pedirEnlace("ana@test.com");
        MockHttpServletResponse noExiste = pedirEnlace("nadie@test.com");

        assertThat(noExiste.getStatus(), is(equalTo(existe.getStatus())));
        assertThat(noExiste.getContentAsString(), is(equalTo(existe.getContentAsString())));
    }

    @Test
    void testOlvidePasswordSinEmailDa400() throws Exception {
        MockHttpServletResponse respuesta = pedirEnlace("");

        assertThat(respuesta.getStatus(), is(400));
        verify(service, never()).solicitarRestablecimiento(anyString());
    }

    @Test
    void testOlvidePasswordConUnEmailMalEscritoDa400() throws Exception {
        MockHttpServletResponse respuesta = pedirEnlace("esto-no-es-un-correo");

        assertThat(respuesta.getStatus(), is(400));
        verify(service, never()).solicitarRestablecimiento(anyString());
    }

    @Test
    void testOlvidePasswordDemasiadasPeticionesDa429() throws Exception {
        doThrow(new DemasiadasPeticionesException("Demasiados intentos")).when(limitador).comprobar(anyString());

        MockHttpServletResponse respuesta = pedirEnlace("ana@test.com");

        assertThat(respuesta.getStatus(), is(429));
        verify(service, never()).solicitarRestablecimiento(anyString());
    }

    // ---------- /restablecer-password ----------

    @Test
    void testRestablecerDevuelve204YPasaLosDatosAlServicio() throws Exception {
        MockHttpServletResponse respuesta = restablecer("mi-token", "Nueva1234!");

        assertThat(respuesta.getStatus(), is(204));
        verify(service).restablecer("mi-token", "Nueva1234!");
    }

    @Test
    void testRestablecerCompruebaElLimiteDePeticiones() throws Exception {
        restablecer("mi-token", "Nueva1234!");

        verify(limitador).comprobar(org.mockito.ArgumentMatchers.startsWith("restablecer:"));
    }

    @Test
    void testRestablecerConTokenInvalidoDa400ConElMensaje() throws Exception {
        doThrow(new TokenRecuperacionInvalidoException()).when(service).restablecer(anyString(), anyString());

        MockHttpServletResponse respuesta = restablecer("falso", "Nueva1234!");

        assertThat(respuesta.getStatus(), is(400));
        assertThat(respuesta.getContentAsString(), containsString("no es válido o ha caducado"));
    }

    @Test
    void testRestablecerConLaMismaContrasenaDa400ConElMensaje() throws Exception {
        doThrow(new UsuarioExceptionPasswordInvalida("La nueva contraseña debe ser distinta de la actual"))
                .when(service).restablecer(anyString(), anyString());

        MockHttpServletResponse respuesta = restablecer("mi-token", "Igual1234!");

        assertThat(respuesta.getStatus(), is(400));
        assertThat(respuesta.getContentAsString(), containsString("distinta de la actual"));
    }

    @Test
    void testRestablecerConUnaContrasenaDebilDa400() throws Exception {
        MockHttpServletResponse respuesta = restablecer("mi-token", "123");

        assertThat(respuesta.getStatus(), is(400));
        verify(service, never()).restablecer(anyString(), anyString());
    }

    @Test
    void testRestablecerSinTokenDa400() throws Exception {
        MockHttpServletResponse respuesta = restablecer("", "Nueva1234!");

        assertThat(respuesta.getStatus(), is(400));
        verify(service, never()).restablecer(anyString(), anyString());
    }

    @Test
    void testElTokenYLaContrasenaNoSalenEnElToString() {
        String texto = new RestablecerPasswordDTORequest("token-secreto", "Nueva1234!").toString();

        assertThat(texto, is(not(containsString("token-secreto"))));
        assertThat(texto, is(not(containsString("Nueva1234!"))));
    }
}
