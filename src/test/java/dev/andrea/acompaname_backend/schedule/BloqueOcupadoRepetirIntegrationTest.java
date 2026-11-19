package dev.andrea.acompaname_backend.schedule;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
import org.springframework.test.web.servlet.ResultActions;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La aplicación completa con MySQL de verdad: repetir un turno guarda los bloques,
// repetirlo otra vez no los duplica y solo lo puede hacer una persona cuidadora.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class BloqueOcupadoRepetirIntegrationTest extends ContenedorMySqlAplicacion {

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
    private LocalDate proximoLunes;

    private RoleEntity rol(String nombre) {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName(nombre).orElseThrow();
        RoleEntity copia = new RoleEntity();
        copia.setId(deLaBaseDeDatos.getId());
        copia.setName(deLaBaseDeDatos.getName());
        return copia;
    }

    private UsuarioEntity crearUsuario(String nombre, String email, String nombreRol) {
        return usuarioRepository.save(UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password(passwordEncoder.encode("Secreta123"))
                .roles(Set.of(rol(nombreRol)))
                .build());
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
        familia = crearUsuario("Ana", "ana.bloq.rep@test.com", "FAMILIA");
        pepe = crearUsuario("Pepe", "pepe.bloq.rep@test.com", "CUIDADOR");
        lucia = crearUsuario("Lucía", "lucia.bloq.rep@test.com", "CUIDADOR");
        perfilPepe = crearPerfil(pepe);
        perfilLucia = crearPerfil(lucia);
        proximoLunes = LocalDate.now().plusDays(1);
        while (proximoLunes.getDayOfWeek() != DayOfWeek.MONDAY) {
            proximoLunes = proximoLunes.plusDays(1);
        }
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

    // Turno de mañana de lunes a viernes durante una semana
    private String cuerpoDeLaSemana(String horaInicio, String horaFin) {
        return "{\"desde\":\"" + proximoLunes + "\",\"hasta\":\"" + proximoLunes.plusDays(6)
                + "\",\"diasSemana\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"],"
                + "\"horaInicio\":\"" + horaInicio + "\",\"horaFin\":\"" + horaFin
                + "\",\"motivo\":\"Turno\"}";
    }

    private ResultActions repetir(UsuarioEntity quien, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/bloques-ocupados/repetir")
                .header("Authorization", "Bearer " + token(quien))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    @Test
    void testUnaPersonaCuidadoraRepiteUnTurnoYVeSusBloques() throws Exception {
        repetir(pepe, cuerpoDeLaSemana("07:00", "15:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(5))
                .andExpect(jsonPath("$.duplicados").value(0));

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].motivo").value("Turno"));
    }

    @Test
    void testRepetirElMismoTurnoDosVecesNoDuplicaLosBloques() throws Exception {
        repetir(pepe, cuerpoDeLaSemana("07:00", "15:00")).andExpect(jsonPath("$.creados").value(5));

        repetir(pepe, cuerpoDeLaSemana("07:00", "15:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.duplicados").value(5));

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void testUnTurnoDeNocheCreaDosBloquesPorNoche() throws Exception {
        repetir(pepe, cuerpoDeLaSemana("22:00", "07:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(10));
    }

    @Test
    void testLosBloquesSonSoloDeQuienLosCrea() throws Exception {
        repetir(pepe, cuerpoDeLaSemana("07:00", "15:00")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(lucia)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testUnaFechaEnElPasadoDa400() throws Exception {
        String cuerpo = "{\"desde\":\"" + LocalDate.now().minusDays(2) + "\",\"hasta\":\""
                + LocalDate.now().plusDays(3) + "\",\"diasSemana\":[\"MONDAY\"],"
                + "\"horaInicio\":\"07:00\",\"horaFin\":\"15:00\"}";

        repetir(pepe, cuerpo).andExpect(status().isBadRequest());
    }

    @Test
    void testUnaFamiliaNoPuedeRepetir() throws Exception {
        repetir(familia, cuerpoDeLaSemana("07:00", "15:00")).andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedeRepetir() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados/repetir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoDeLaSemana("07:00", "15:00")))
                .andExpect(status().isUnauthorized());
    }
}
