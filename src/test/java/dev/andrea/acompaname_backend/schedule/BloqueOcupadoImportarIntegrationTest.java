package dev.andrea.acompaname_backend.schedule;

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

// La aplicación completa con MySQL de verdad: importar un calendario guarda los bloques,
// importarlo otra vez no los duplica y solo lo puede hacer una persona cuidadora.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class BloqueOcupadoImportarIntegrationTest extends ContenedorMySqlAplicacion {

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
        familia = crearUsuario("Ana", "ana.bloq.imp@test.com", "FAMILIA");
        pepe = crearUsuario("Pepe", "pepe.bloq.imp@test.com", "CUIDADOR");
        lucia = crearUsuario("Lucía", "lucia.bloq.imp@test.com", "CUIDADOR");
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

    // El cuerpo de la petición: un calendario con un evento de 09:00 a 11:00 dentro de tres días
    private String cuerpo() {
        String dia = LocalDate.now().plusDays(3).toString().replace("-", "");
        return "{\"contenido\":\"BEGIN:VCALENDAR\\r\\nBEGIN:VEVENT\\r\\nSUMMARY:Cita privada\\r\\nDTSTART:" + dia
                + "T090000\\r\\nDTEND:" + dia + "T110000\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\"}";
    }

    private org.springframework.test.web.servlet.ResultActions importar(UsuarioEntity quien) throws Exception {
        return mockMvc.perform(post("/api/v1/bloques-ocupados/importar")
                .header("Authorization", "Bearer " + token(quien))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo()));
    }

    @Test
    void testUnaPersonaCuidadoraImportaYVeSusBloques() throws Exception {
        importar(pepe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importados").value(1))
                .andExpect(jsonPath("$.duplicados").value(0));

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].motivo").value("Calendario importado"));
    }

    @Test
    void testImportarElMismoArchivoDosVecesNoDuplicaLosBloques() throws Exception {
        importar(pepe).andExpect(jsonPath("$.importados").value(1));

        importar(pepe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importados").value(0))
                .andExpect(jsonPath("$.duplicados").value(1));

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testLosBloquesImportadosSonSoloDeQuienImporta() throws Exception {
        importar(pepe).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bloques-ocupados")
                .header("Authorization", "Bearer " + token(lucia)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testUnArchivoQueNoEsUnCalendarioDa400() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados/importar")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contenido\":\"esto no es un calendario\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnaFamiliaNoPuedeImportar() throws Exception {
        importar(familia).andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedeImportar() throws Exception {
        mockMvc.perform(post("/api/v1/bloques-ocupados/importar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo()))
                .andExpect(status().isUnauthorized());
    }
}
