package dev.andrea.acompaname_backend.suggestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// La aplicación completa con MySQL de verdad: quién puede enviar sugerencias y quién puede leerlas.
// El límite de peticiones se sube a 1000 para que estos tests no lo alcancen.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class SugerenciaSecurityIntegrationTest extends ContenedorMySqlAplicacion {

    private static final String CUERPO = "{\"texto\":\"Me gustaría poder cambiar el color de la web\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SugerenciaRepository sugerenciaRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private UsuarioEntity familia;
    private UsuarioEntity cuidador;
    private UsuarioEntity admin;

    // Copia simple del rol (solo id y nombre), como en SecurityJwtIntegrationTest
    private RoleEntity rol(String nombre) {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName(nombre).orElseThrow();
        RoleEntity copia = new RoleEntity();
        copia.setId(deLaBaseDeDatos.getId());
        copia.setName(deLaBaseDeDatos.getName());
        return copia;
    }

    private UsuarioEntity crearUsuario(String nombre, String email, String nombreRol) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password(passwordEncoder.encode("Secreta123"))
                .roles(Set.of(rol(nombreRol)))
                .build();
        return usuarioRepository.save(usuario);
    }

    @BeforeEach
    void crearPersonas() {
        familia = crearUsuario("Ana", "ana.sug.sec@test.com", "FAMILIA");
        cuidador = crearUsuario("Pepe", "pepe.sug.sec@test.com", "CUIDADOR");
        admin = crearUsuario("Admin", "admin.sug.sec@test.com", "ADMIN");
    }

    @AfterEach
    void borrarTodo() {
        sugerenciaRepository.deleteAll();
        usuarioRepository.delete(familia);
        usuarioRepository.delete(cuidador);
        usuarioRepository.delete(admin);
    }

    private String token(UsuarioEntity usuario) {
        return jwtService.crearToken(usuario);
    }

    @Test
    void testUnaFamiliaPuedeEnviarUnaSugerencia() throws Exception {
        mockMvc.perform(post("/api/v1/sugerencias")
                .header("Authorization", "Bearer " + token(familia))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Me gustaría poder cambiar el color de la web"));
    }

    @Test
    void testUnaPersonaCuidadoraPuedeEnviarUnaSugerencia() throws Exception {
        mockMvc.perform(post("/api/v1/sugerencias")
                .header("Authorization", "Bearer " + token(cuidador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
                .andExpect(status().isCreated());
    }

    @Test
    void testElAdministradorNoEnviaSugerencias() throws Exception {
        mockMvc.perform(post("/api/v1/sugerencias")
                .header("Authorization", "Bearer " + token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedeEnviarUnaSugerencia() throws Exception {
        mockMvc.perform(post("/api/v1/sugerencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testElAdministradorVeLasSugerenciasConSuAutor() throws Exception {
        mockMvc.perform(post("/api/v1/sugerencias")
                .header("Authorization", "Bearer " + token(familia))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/sugerencias")
                .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Me gustaría poder cambiar el color de la web"))
                .andExpect(jsonPath("$[0].nombreUsuario").value("Ana"))
                .andExpect(jsonPath("$[0].emailUsuario").value("ana.sug.sec@test.com"))
                .andExpect(jsonPath("$[0].rol").value("FAMILIA"));
    }

    @Test
    void testUnaFamiliaNoPuedeLeerLasSugerencias() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sugerencias")
                .header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnaPersonaCuidadoraNoPuedeLeerLasSugerencias() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sugerencias")
                .header("Authorization", "Bearer " + token(cuidador)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedenLeerLasSugerencias() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sugerencias"))
                .andExpect(status().isUnauthorized());
    }
}
