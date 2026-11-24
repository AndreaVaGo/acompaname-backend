package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La aplicación completa con MySQL de verdad: la foto se pide sin iniciar sesión (como el perfil),
// la lista no lleva la imagen y un perfil sin foto responde 404.
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false" })
@AutoConfigureMockMvc
public class PerfilCuidadorFotoSecurityIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UsuarioEntity conFotoUsuario;
    private UsuarioEntity sinFotoUsuario;
    private PerfilCuidadorEntity conFoto;
    private PerfilCuidadorEntity sinFoto;

    private UsuarioEntity crearCuidador(String nombre, String email) {
        RoleEntity deLaBaseDeDatos = roleRepository.findByName("CUIDADOR").orElseThrow();
        RoleEntity rol = new RoleEntity();
        rol.setId(deLaBaseDeDatos.getId());
        rol.setName(deLaBaseDeDatos.getName());
        return usuarioRepository.save(UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password(passwordEncoder.encode("Secreta123"))
                .roles(Set.of(rol))
                .build());
    }

    private PerfilCuidadorEntity crearPerfil(UsuarioEntity usuario, String foto) {
        return perfilRepository.save(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .foto(foto)
                .usuario(usuario)
                .build());
    }

    @BeforeEach
    void crearPerfiles() {
        conFotoUsuario = crearCuidador("Pepe", "pepe.foto.sec@test.com");
        sinFotoUsuario = crearCuidador("Lucía", "lucia.foto.sec@test.com");
        String foto = "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 });
        conFoto = crearPerfil(conFotoUsuario, foto);
        sinFoto = crearPerfil(sinFotoUsuario, null);
    }

    @AfterEach
    void borrarTodo() {
        perfilRepository.delete(conFoto);
        perfilRepository.delete(sinFoto);
        usuarioRepository.delete(conFotoUsuario);
        usuarioRepository.delete(sinFotoUsuario);
    }

    @Test
    void testLaFotoSePideSinIniciarSesion() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/" + conFoto.getId() + "/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }))
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")));
    }

    @Test
    void testUnPerfilSinFotoDa404() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/" + sinFoto.getId() + "/foto"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUnPerfilQueNoExisteDa404() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/999999999/foto")).andExpect(status().isNotFound());
    }

    @Test
    void testLaListaLlevaLaDireccionDeLaFotoYNoLaImagen() throws Exception {
        String respuesta = mockMvc.perform(get("/api/v1/cuidadores"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.hamcrest.MatcherAssert.assertThat(respuesta, not(containsString("base64")));
        org.hamcrest.MatcherAssert.assertThat(respuesta,
                containsString("/cuidadores/" + conFoto.getId() + "/foto?v="));
    }

    @Test
    void testElPerfilPublicoLlevaLaDireccionDeLaFoto() throws Exception {
        mockMvc.perform(get("/api/v1/cuidadores/" + conFoto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foto", startsWith("/cuidadores/" + conFoto.getId() + "/foto?v=")));
    }
}
