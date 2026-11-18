package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La aplicación completa con MySQL de verdad: solo las personas cuidadoras usan su calendario
// y nadie puede borrar los bloques de otra persona.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class BloqueOcupadoSecurityIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private BloqueOcupadoRepository bloqueRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private UsuarioEntity familia;
    private UsuarioEntity pepe;
    private UsuarioEntity lucia;
    private PerfilCuidadorEntity perfilPepe;
    private PerfilCuidadorEntity perfilLucia;

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

    private PerfilCuidadorEntity crearPerfil(UsuarioEntity usuario) {
        return perfilRepository.save(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build());
    }

    @BeforeEach
    void crearPersonas() {
        familia = crearUsuario("Ana", "ana.bloq.sec@test.com", "FAMILIA");
        pepe = crearUsuario("Pepe", "pepe.bloq.sec@test.com", "CUIDADOR");
        lucia = crearUsuario("Lucía", "lucia.bloq.sec@test.com", "CUIDADOR");
        perfilPepe = crearPerfil(pepe);
        perfilLucia = crearPerfil(lucia);
    }

    @AfterEach
    void borrarTodo() {
        bloqueRepository.deleteAll();
        perfilRepository.delete(perfilPepe);
        perfilRepository.delete(perfilLucia);
        usuarioRepository.delete(familia);
        usuarioRepository.delete(pepe);
        usuarioRepository.delete(lucia);
    }

    private String token(UsuarioEntity usuario) {
        return jwtService.crearToken(usuario);
    }

    private String cuerpo() {
        return "{\"fecha\":\"" + LocalDate.now().plusDays(3)
                + "\",\"horaInicio\":\"08:00\",\"horaFin\":\"15:00\",\"motivo\":\"Turno\"}";
    }

    @Test
    void testUnaPersonaCuidadoraCreaYVeSuBloque() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("Turno"))
                .andExpect(jsonPath("$[0].horaInicio", startsWith("08:00")));
    }

    @Test
    void testCadaPersonaCuidadoraSoloVeSusBloques() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(lucia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testUnaFamiliaNoPuedeCrearBloques() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(familia))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnaFamiliaNoPuedeVerBloques() throws Exception {
        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedenVerBloques() throws Exception {
        mockMvc.perform(get("/api/v1/bloques-ocupados"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnaPersonaCuidadoraBorraSuBloque() throws Exception {
        BloqueOcupadoEntity bloque = bloqueRepository.save(BloqueOcupadoEntity.builder()
                .cuidador(perfilPepe)
                .fecha(LocalDate.now().plusDays(3))
                .horaInicio(java.time.LocalTime.of(8, 0))
                .horaFin(java.time.LocalTime.of(15, 0))
                .build());

        mockMvc.perform(delete("/api/v1/bloques-ocupados/" + bloque.getId())
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isNoContent());
    }

    @Test
    void testNoSePuedeBorrarElBloqueDeOtraPersona() throws Exception {
        BloqueOcupadoEntity bloque = bloqueRepository.save(BloqueOcupadoEntity.builder()
                .cuidador(perfilPepe)
                .fecha(LocalDate.now().plusDays(3))
                .horaInicio(java.time.LocalTime.of(8, 0))
                .horaFin(java.time.LocalTime.of(15, 0))
                .build());

        mockMvc.perform(delete("/api/v1/bloques-ocupados/" + bloque.getId())
                .header("Authorization", "Bearer " + token(lucia)))
                .andExpect(status().isForbidden());
    }
}
