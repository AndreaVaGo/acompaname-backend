package dev.andrea.acompaname_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

// La aplicación completa con un MySQL de verdad (Testcontainers): login, token y qué pasa
// cuando el token está caducado, manipulado, o la persona cambia de rol o la bloquean.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SecurityJwtIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    private static final String EMAIL = "ana.jwt@test.com";
    private static final String PASSWORD = "Secreta123";

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
    private JwtService jwtService;
    @Autowired
    private JwtEncoder jwtEncoder;

    private UsuarioEntity ana;

    // Una copia simple del rol (solo id y nombre). El rol de la base de datos lleva una lista
    // de usuarios que se carga al usarla, y fuera de una sesión daría error
    private RoleEntity rol(String nombre) {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName(nombre).orElseThrow();
        RoleEntity copia = new RoleEntity();
        copia.setId(deLaBaseDeDatos.getId());
        copia.setName(deLaBaseDeDatos.getName());
        return copia;
    }

    // Antes de cada test: Ana, con rol FAMILIA
    @BeforeEach
    void crearAna() {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre("Ana")
                .email(EMAIL)
                .telefono("600111222")
                .password(passwordEncoder.encode(PASSWORD))
                .roles(Set.of(rol("FAMILIA")))
                .build();
        ana = usuarioRepository.save(usuario);
    }

    @AfterEach
    void borrarAna() {
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
    }

    private String cuerpoDeLogin(String email, String password) throws Exception {
        return mapper.writeValueAsString(new LoginDTORequest(email, password));
    }

    // GET /usuarios solo pide estar logueado: sirve para comprobar si un token vale
    private int pedirUsuariosConToken(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/usuarios")
                .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    // POST /solicitudes solo deja pasar a FAMILIA. Con "{}" un FAMILIA recibe 400 (pasó la
    // seguridad pero faltan datos) y cualquier otro rol recibe 403
    private int crearSolicitudVaciaConToken(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/solicitudes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andReturn().getResponse().getStatus();
    }

    // ---------- Login ----------

    @Test
    void testLoginCorrectoDevuelveTokenYSirveParaPedirDatos() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLogin(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(EMAIL))
                .andExpect(jsonPath("$.roles").value("FAMILIA"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");

        assertEquals(200, pedirUsuariosConToken(token));
    }

    @Test
    void testElLoginNoCreaSesionNiCookie() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLogin(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void testLoginConContrasenaIncorrectaDa401() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLogin(EMAIL, "otra-contrasena")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginConEmailQueNoExisteDa401() throws Exception {
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLogin("nadie@test.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnaPersonaBloqueadaNoPuedeHacerLogin() throws Exception {
        ana.setActivo(false);
        usuarioRepository.save(ana);

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLogin(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Peticiones con token ----------

    @Test
    void testSinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnTokenInventadoDa401() throws Exception {
        assertEquals(401, pedirUsuariosConToken("esto-no-es-un-token"));
    }

    @Test
    void testUnTokenManipuladoDa401() throws Exception {
        String token = jwtService.crearToken(ana);
        String manipulado = token.substring(0, token.length() - 8) + "AAAAAAAA";

        assertEquals(401, pedirUsuariosConToken(manipulado));
    }

    @Test
    void testUnTokenCaducadoDa401() throws Exception {
        Instant ahora = Instant.now();
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("acompaname")
                .issuedAt(ahora.minusSeconds(600))
                .expiresAt(ahora.minusSeconds(300))
                .subject(String.valueOf(ana.getId()))
                .build();
        String caducado = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();

        assertEquals(401, pedirUsuariosConToken(caducado));
    }

    // El Basic de antes ya no vale
    @Test
    void testElBasicAuthYaNoFunciona() throws Exception {
        String basic = Base64.getEncoder().encodeToString((EMAIL + ":" + PASSWORD).getBytes());

        mockMvc.perform(get("/api/v1/usuarios")
                .header("Authorization", "Basic " + basic))
                .andExpect(status().isUnauthorized());
    }

    // ---------- El token no decide por sí solo: se mira la base de datos en cada petición ----------

    @Test
    void testSiBloqueanALaPersonaSuTokenDejaDeValerAlMomento() throws Exception {
        String token = jwtService.crearToken(ana);
        assertEquals(200, pedirUsuariosConToken(token));

        ana.setActivo(false);
        usuarioRepository.save(ana);

        assertEquals(401, pedirUsuariosConToken(token));
    }

    @Test
    void testSiBorranALaPersonaSuTokenDejaDeValer() throws Exception {
        String token = jwtService.crearToken(ana);
        usuarioRepository.delete(ana);

        assertEquals(401, pedirUsuariosConToken(token));
    }

    @Test
    void testSiCambiaElRolDeLaPersonaSeNotaConElMismoToken() throws Exception {
        String token = jwtService.crearToken(ana);
        // Como FAMILIA pasa la seguridad (400 porque faltan datos)
        assertEquals(400, crearSolicitudVaciaConToken(token));

        ana.setRoles(Set.of(rol("CUIDADOR")));
        usuarioRepository.save(ana);

        // Con el mismo token, ahora es CUIDADOR y no puede crear solicitudes
        assertEquals(403, crearSolicitudVaciaConToken(token));
    }
}