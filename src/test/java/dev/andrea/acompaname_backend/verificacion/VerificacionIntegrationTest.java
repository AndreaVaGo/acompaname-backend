package dev.andrea.acompaname_backend.verificacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

// La aplicación completa con MySQL de verdad: la persona cuidadora envía, el administrador resuelve
// y cualquiera ve solo el "sí/no" de las verificaciones aprobadas.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class VerificacionIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private VerificacionRepository verificacionRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private UsuarioEntity familia;
    private UsuarioEntity admin;
    private UsuarioEntity pepe;
    private PerfilCuidadorEntity perfilPepe;

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

    @BeforeEach
    void crearPersonas() {
        familia = crearUsuario("Ana", "ana.verif@test.com", "FAMILIA");
        admin = crearUsuario("Admin", "admin.verif@test.com", "ADMIN");
        pepe = crearUsuario("Pepe", "pepe.verif@test.com", "CUIDADOR");
        perfilPepe = perfilRepository.save(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(pepe)
                .build());
    }

    @AfterEach
    void borrarTodo() {
        verificacionRepository.deleteAll();
        perfilRepository.delete(perfilPepe);
        usuarioRepository.delete(familia);
        usuarioRepository.delete(admin);
        usuarioRepository.delete(pepe);
    }

    private String token(UsuarioEntity usuario) {
        return jwtService.crearToken(usuario);
    }

    @Test
    void testFlujoCompletoEnviarAprobarYVerEnPublico() throws Exception {
        mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"COL-12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // Mientras está pendiente no aparece como verificada
        mockMvc.perform(get("/api/v1/verificaciones/publicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Long id = verificacionRepository.findByCuidadorId(perfilPepe.getId()).get(0).getId();
        mockMvc.perform(put("/api/v1/admin/verificaciones/" + id)
                .header("Authorization", "Bearer " + token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VERIFICADO"));

        // Sin iniciar sesión se ve el sí/no y NUNCA la referencia
        mockMvc.perform(get("/api/v1/verificaciones/publicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuidadorId").value(perfilPepe.getId()))
                .andExpect(jsonPath("$[0].titulacionVerificada").value(true))
                .andExpect(jsonPath("$[0].antecedentesVerificados").value(false))
                .andExpect(jsonPath("$[0].referencia").doesNotExist());
    }

    @Test
    void testUnaFamiliaNoPuedeEnviarNiVerLasVerificaciones() throws Exception {
        mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .header("Authorization", "Bearer " + token(familia))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"COL-12345\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/verificaciones/mias")
                .header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedenVerLasPropias() throws Exception {
        mockMvc.perform(get("/api/v1/verificaciones/mias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSoloElAdministradorPuedeResolver() throws Exception {
        mockMvc.perform(put("/api/v1/admin/verificaciones/1")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/verificaciones")
                .header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testElAdministradorVePendientesConElNombreYLaReferencia() throws Exception {
        mockMvc.perform(put("/api/v1/verificaciones/ANTECEDENTES")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"SD:ABC123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/verificaciones?estado=PENDIENTE")
                .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuidadorNombre").value("Pepe"))
                .andExpect(jsonPath("$[0].referencia").value("SD:ABC123"))
                .andExpect(jsonPath("$[0].tipo").value("ANTECEDENTES"));
    }

    @Test
    void testRechazarYReenviarVuelveAPendiente() throws Exception {
        mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"MAL\"}"))
                .andExpect(status().isOk());
        Long id = verificacionRepository.findByCuidadorId(perfilPepe.getId()).get(0).getId();

        mockMvc.perform(put("/api/v1/admin/verificaciones/" + id)
                .header("Authorization", "Bearer " + token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprobar\":false,\"motivo\":\"No existe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        mockMvc.perform(get("/api/v1/verificaciones/mias")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(jsonPath("$[0].motivoRechazo").value("No existe"));

        mockMvc.perform(put("/api/v1/verificaciones/TITULACION")
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"BIEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.motivoRechazo").doesNotExist());
    }
}
