package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

// El recorrido completo con la aplicación y un MySQL de verdad: pedir el enlace, leer el token
// del correo, poner la contraseña nueva y entrar con ella. Solo el envío del correo es falso.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class RecuperacionPasswordIntegrationTest extends ContenedorMySqlAplicacion {

    private static final String EMAIL = "ana.recuperar@test.com";
    private static final String PASSWORD_VIEJA = "Vieja1234!";
    private static final String PASSWORD_NUEVA = "Nueva1234!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean
    private ServicioDeCorreo correo;

    private UsuarioEntity ana;

    private RoleEntity rol(String nombre) {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName(nombre).orElseThrow();
        RoleEntity copia = new RoleEntity();
        copia.setId(deLaBaseDeDatos.getId());
        copia.setName(deLaBaseDeDatos.getName());
        return copia;
    }

    @BeforeEach
    void crearAna() {
        ana = usuarioRepository.save(UsuarioEntity.builder()
                .nombre("Ana")
                .email(EMAIL)
                .telefono("600111222")
                .password(passwordEncoder.encode(PASSWORD_VIEJA))
                .roles(Set.of(rol("FAMILIA")))
                .build());
    }

    @AfterEach
    void borrarAna() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
    }

    private int pedirEnlace(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/olvide-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new OlvidePasswordDTORequest(email))))
                .andReturn().getResponse().getStatus();
    }

    private int restablecer(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new RestablecerPasswordDTORequest(token, password))))
                .andReturn().getResponse().getStatus();
    }

    private int login(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginDTORequest(EMAIL, password))))
                .andReturn().getResponse().getStatus();
    }

    // Lee el correo "enviado" y saca el token del enlace
    private String tokenDelUltimoCorreo() {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo, org.mockito.Mockito.atLeastOnce()).enviar(anyString(), anyString(), texto.capture());
        String ultimo = texto.getValue();
        return ultimo.split("token=")[1].split("\\s")[0];
    }

    @Test
    void testRecorridoCompletoCambiaLaContrasena() throws Exception {
        assertThat(pedirEnlace(EMAIL), is(202));
        String token = tokenDelUltimoCorreo();

        assertThat(restablecer(token, PASSWORD_NUEVA), is(204));

        assertThat(login(PASSWORD_NUEVA), is(200));
        assertThat(login(PASSWORD_VIEJA), is(401));
    }

    @Test
    void testElEnlaceSoloSePuedeUsarUnaVez() throws Exception {
        pedirEnlace(EMAIL);
        String token = tokenDelUltimoCorreo();

        assertThat(restablecer(token, PASSWORD_NUEVA), is(204));
        assertThat(restablecer(token, "Otra12345!"), is(400));

        // La contraseña sigue siendo la primera que se puso
        assertThat(login(PASSWORD_NUEVA), is(200));
    }

    @Test
    void testSoloValeElUltimoEnlacePedido() throws Exception {
        pedirEnlace(EMAIL);
        String primero = tokenDelUltimoCorreo();
        org.mockito.Mockito.clearInvocations(correo);
        pedirEnlace(EMAIL);
        String segundo = tokenDelUltimoCorreo();

        assertThat(restablecer(primero, PASSWORD_NUEVA), is(400));
        assertThat(restablecer(segundo, PASSWORD_NUEVA), is(204));
    }

    @Test
    void testEnLaBaseDeDatosSoloSeGuardaElHash() throws Exception {
        pedirEnlace(EMAIL);
        String token = tokenDelUltimoCorreo();

        assertThat(tokenRepository.findByTokenHash(token).isPresent(), is(false));
        assertThat(tokenRepository.findByTokenHash(CodificadorDeTokens.hash(token)).isPresent(), is(true));
    }

    @Test
    void testUnTokenInventadoDa400() throws Exception {
        assertThat(restablecer("token-inventado", PASSWORD_NUEVA), is(400));
    }

    @Test
    void testUnTokenCaducadoDa400() throws Exception {
        pedirEnlace(EMAIL);
        String token = tokenDelUltimoCorreo();
        PasswordResetTokenEntity guardado = tokenRepository.findByTokenHash(CodificadorDeTokens.hash(token))
                .orElseThrow();
        guardado.setExpiraEn(java.time.LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(guardado);

        assertThat(restablecer(token, PASSWORD_NUEVA), is(400));
        assertThat(login(PASSWORD_VIEJA), is(200));
    }

    @Test
    void testLaMismaContrasenaDa400YElEnlaceSigueValiendo() throws Exception {
        pedirEnlace(EMAIL);
        String token = tokenDelUltimoCorreo();

        assertThat(restablecer(token, PASSWORD_VIEJA), is(400));
        // Como no se cambió nada, el enlace todavía sirve
        assertThat(restablecer(token, PASSWORD_NUEVA), is(204));
    }

    @Test
    void testUnCorreoQueNoExisteContestaIgualYNoEnviaNada() throws Exception {
        assertThat(pedirEnlace("nadie@test.com"), is(equalTo(pedirEnlace(EMAIL))));

        // Solo se mandó el correo de Ana; el de "nadie" no generó ninguno
        verify(correo, org.mockito.Mockito.times(1)).enviar(anyString(), anyString(), anyString());
        verify(correo, never()).enviar(org.mockito.ArgumentMatchers.eq("nadie@test.com"), anyString(), anyString());
    }

    @Test
    void testUnaCuentaBloqueadaNoRecibeCorreo() throws Exception {
        ana.setActivo(false);
        usuarioRepository.save(ana);

        assertThat(pedirEnlace(EMAIL), is(202));

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void testLosDosEndpointsSonPublicosYNoPidenToken() throws Exception {
        // Sin cabecera Authorization: nunca debe salir 401 ni 403
        assertThat(pedirEnlace(EMAIL), is(202));
        assertThat(restablecer("falso", PASSWORD_NUEVA), is(400));
    }
}
