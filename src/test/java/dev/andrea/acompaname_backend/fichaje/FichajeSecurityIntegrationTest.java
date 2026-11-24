package dev.andrea.acompaname_backend.fichaje;

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
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La aplicación completa con MySQL de verdad: solo la persona cuidadora de la solicitud ficha,
// la familia y la persona cuidadora ven los fichajes, y nadie más.
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false", "fichaje.verificar-lugar=true" })
@AutoConfigureMockMvc
public class FichajeSecurityIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private FichajeRepository fichajeRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private UsuarioEntity familia;
    private UsuarioEntity otraFamilia;
    private UsuarioEntity pepe;
    private UsuarioEntity lucia;
    private PerfilCuidadorEntity perfilPepe;
    private PerfilCuidadorEntity perfilLucia;
    private SolicitudEntity solicitud;

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
        familia = crearUsuario("Ana", "ana.fichaje.sec@test.com", "FAMILIA");
        otraFamilia = crearUsuario("Eva", "eva.fichaje.sec@test.com", "FAMILIA");
        pepe = crearUsuario("Pepe", "pepe.fichaje.sec@test.com", "CUIDADOR");
        lucia = crearUsuario("Lucía", "lucia.fichaje.sec@test.com", "CUIDADOR");
        perfilPepe = crearPerfil(pepe);
        perfilLucia = crearPerfil(lucia);
        solicitud = solicitudRepository.save(SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.ACEPTADA)
                .fechaCuidado(LocalDate.now())
                .direccion("Calle Uría 1, Oviedo")
                .familia(familia)
                .cuidador(perfilPepe)
                .build());
    }

    @AfterEach
    void borrarTodo() {
        fichajeRepository.deleteAll();
        solicitudRepository.delete(solicitud);
        perfilRepository.delete(perfilPepe);
        perfilRepository.delete(perfilLucia);
        usuarioRepository.delete(familia);
        usuarioRepository.delete(otraFamilia);
        usuarioRepository.delete(pepe);
        usuarioRepository.delete(lucia);
    }

    private String token(UsuarioEntity usuario) {
        return jwtService.crearToken(usuario);
    }

    private String ruta(String sufijo) {
        return "/api/v1/fichajes/" + solicitud.getId() + sufijo;
    }

    @Test
    void testLaPersonaCuidadoraEmpiezaYTerminaElServicio() throws Exception {
        mockMvc.perform(post(ruta("/inicio"))
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitud\":43.36,\"longitud\":-5.85}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INICIO"))
                // Con el geocodificador desactivado la dirección no se puede situar en el mapa
                .andExpect(jsonPath("$.ubicacion").value("NO_VERIFICABLE"));

        mockMvc.perform(post(ruta("/fin"))
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("FIN"))
                .andExpect(jsonPath("$.ubicacion").value("SIN_UBICACION"));
    }

    @Test
    void testNoSePuedeEmpezarDosVecesPorLaApi() throws Exception {
        mockMvc.perform(post(ruta("/inicio")).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ruta("/inicio")).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isConflict());
    }

    @Test
    void testNoSePuedeTerminarSinEmpezarPorLaApi() throws Exception {
        mockMvc.perform(post(ruta("/fin")).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUnaFamiliaNoPuedeFichar() throws Exception {
        mockMvc.perform(post(ruta("/inicio")).header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOtraPersonaCuidadoraNoPuedeFicharEnUnaSolicitudAjena() throws Exception {
        mockMvc.perform(post(ruta("/inicio")).header("Authorization", "Bearer " + token(lucia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionNoSePuedeFicharNiVer() throws Exception {
        mockMvc.perform(post(ruta("/inicio"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ruta(""))).andExpect(status().isUnauthorized());
    }

    @Test
    void testLaFamiliaYLaPersonaCuidadoraVenLosFichajes() throws Exception {
        mockMvc.perform(post(ruta("/inicio")).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ruta("")).header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("INICIO"))
                // Nunca se devuelven coordenadas
                .andExpect(jsonPath("$[0].latitud").doesNotExist())
                .andExpect(jsonPath("$[0].longitud").doesNotExist());

        mockMvc.perform(get(ruta("")).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testOtraFamiliaYOtraPersonaCuidadoraNoVenLosFichajes() throws Exception {
        mockMvc.perform(get(ruta("")).header("Authorization", "Bearer " + token(otraFamilia)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ruta("")).header("Authorization", "Bearer " + token(lucia)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLaConfiguracionLaVeCualquierPersonaConSesion() throws Exception {
        mockMvc.perform(get("/api/v1/fichajes/configuracion").header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificarLugar").value(true));

        mockMvc.perform(get("/api/v1/fichajes/configuracion")).andExpect(status().isUnauthorized());
    }

    @Test
    void testFicharEnUnaSolicitudQueNoExisteDa404() throws Exception {
        mockMvc.perform(post("/api/v1/fichajes/999999/inicio").header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isNotFound());
    }
}
