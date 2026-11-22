package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.security.DemasiadasPeticionesException;
import dev.andrea.acompaname_backend.security.LimitadorDePeticiones;
import dev.andrea.acompaname_backend.security.SecurityUser;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DosPasosController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DosPasosControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    private DosPasosService service;
    @MockitoBean
    private LimitadorDePeticiones limitador;

    // La persona que "está dentro" (en un test real la pondría el filtro del token)
    private UsernamePasswordAuthenticationToken persona(boolean dosPasos) {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        UsuarioEntity usuario = new UsuarioEntity(5L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol));
        usuario.setDosPasos(dosPasos);
        SecurityUser securityUser = new SecurityUser(usuario);
        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private MockHttpServletResponse cambiar(Object cuerpo) throws Exception {
        return mockMvc.perform(put("/api/v1/dos-pasos")
                .principal(persona(false))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(cuerpo)))
                .andReturn()
                .getResponse();
    }

    @Test
    void testElEstadoDiceQueEstaDesactivadoPorDefecto() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/dos-pasos").principal(persona(false)))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo("{\"activo\":false}")));
    }

    @Test
    void testElEstadoDiceQueEstaActivadoSiLaPersonaLoTiene() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/dos-pasos").principal(persona(true)))
                .andReturn().getResponse();

        assertThat(response.getContentAsString(), is(equalTo("{\"activo\":true}")));
    }

    @Test
    void testActivarLlamaAlServicioConElIdDelTokenYDevuelveElNuevoEstado() throws Exception {
        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(true, "1234"));

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo("{\"activo\":true}")));
        Mockito.verify(service).cambiarEstado(5L, true, "1234");
    }

    @Test
    void testDesactivarLlamaAlServicio() throws Exception {
        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(false, "1234"));

        assertThat(response.getContentAsString(), is(equalTo("{\"activo\":false}")));
        Mockito.verify(service).cambiarEstado(5L, false, "1234");
    }

    @Test
    void testConLaContrasenaIncorrectaDa400() throws Exception {
        Mockito.doThrow(new DosPasosException("La contraseña no es correcta"))
                .when(service).cambiarEstado(5L, true, "mala");

        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(true, "mala"));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La contraseña no es correcta"));
    }

    @Test
    void testSinContrasenaDa400YNoSeLlamaAlServicio() throws Exception {
        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(true, ""));

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testSinIndicarActivoDa400YNoSeLlamaAlServicio() throws Exception {
        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(null, "1234"));

        assertThat(response.getStatus(), is(equalTo(400)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testCambiarPreguntaAlLimitadorPorPersona() throws Exception {
        cambiar(new CambiarDosPasosDTORequest(true, "1234"));

        Mockito.verify(limitador).comprobar("dos-pasos:5");
    }

    @Test
    void testConDemasiadosIntentosDa429() throws Exception {
        Mockito.doThrow(new DemasiadasPeticionesException("Demasiados intentos."))
                .when(limitador).comprobar(ArgumentMatchers.anyString());

        MockHttpServletResponse response = cambiar(new CambiarDosPasosDTORequest(true, "1234"));

        assertThat(response.getStatus(), is(equalTo(429)));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testLaContrasenaNoSaleEnElToString() {
        String texto = new CambiarDosPasosDTORequest(true, "clave-secreta").toString();

        assertThat(texto, not(containsString("clave-secreta")));
        assertThat(texto, containsString("***"));
    }
}
