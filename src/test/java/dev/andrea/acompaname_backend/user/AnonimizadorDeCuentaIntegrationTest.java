package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.chat.MensajeEntity;
import dev.andrea.acompaname_backend.chat.MensajeRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenEntity;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.suggestion.SugerenciaEntity;
import dev.andrea.acompaname_backend.suggestion.SugerenciaRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Con MySQL de verdad: borrar la cuenta de una persona que tiene historial (solicitudes, mensajes...)
// ya no choca con las claves foráneas, y la otra parte conserva su historial.
@SpringBootTest(properties = { "rate-limit.max-requests=1000", "geocoding.enabled=false" })
public class AnonimizadorDeCuentaIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private AnonimizadorDeCuenta anonimizador;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private MensajeRepository mensajeRepository;
    @Autowired
    private SugerenciaRepository sugerenciaRepository;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UsuarioEntity familia;
    private UsuarioEntity cuidador;
    private PerfilCuidadorEntity perfil;
    private SolicitudEntity solicitud;
    private MensajeEntity mensaje;

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
    void crearHistorial() {
        familia = crearUsuario("Ana", "ana.anonimizar@test.com", "FAMILIA");
        cuidador = crearUsuario("Pepe", "pepe.anonimizar@test.com", "CUIDADOR");
        perfil = perfilRepository.save(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Llevo años cuidando mayores")
                .disponibleAhora(true)
                .foto("data:image/png;base64,AAAA")
                .ciudad("Oviedo")
                .usuario(cuidador)
                .build());
        solicitud = solicitudRepository.save(SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.COMPLETADA)
                .fechaCuidado(LocalDate.now().minusDays(3))
                .direccion("Calle Uría 1, Oviedo")
                .familia(familia)
                .cuidador(perfil)
                .build());
        mensaje = mensajeRepository.save(MensajeEntity.builder()
                .solicitud(solicitud)
                .emisor(familia)
                .texto("Hola, ¿a qué hora llegas?")
                .fecha(LocalDateTime.now())
                .leido(true)
                .build());
        sugerenciaRepository.save(SugerenciaEntity.builder()
                .texto("Estaría bien poder cambiar la hora")
                .fecha(LocalDateTime.now())
                .usuario(familia)
                .build());
        tokenRepository.save(PasswordResetTokenEntity.builder()
                .tokenHash("a".repeat(64))
                .usuario(familia)
                .expiraEn(LocalDateTime.now().plusHours(1))
                .build());
    }

    @AfterEach
    void borrarTodo() {
        // Las sugerencias de la familia (si el test no las borró ya) no tienen borrado en cascada
        sugerenciaRepository.findAll().stream()
                .filter(s -> s.getUsuario() != null && s.getUsuario().getId().equals(familia.getId()))
                .forEach(sugerenciaRepository::delete);
        mensajeRepository.delete(mensaje);
        solicitudRepository.delete(solicitud);
        perfilRepository.delete(perfil);
        usuarioRepository.delete(familia);
        usuarioRepository.delete(cuidador);
    }

    @Test
    void anonimizarUnaFamiliaConHistorialNoRompeNadaYConservaLaSolicitudYElMensaje() {
        anonimizador.anonimizar(usuarioRepository.findById(familia.getId()).orElseThrow());

        UsuarioEntity despues = usuarioRepository.findById(familia.getId()).orElseThrow();
        assertThat(despues.getNombre(), is(equalTo("Cuenta eliminada")));
        assertThat(despues.getEmail(), is(equalTo("eliminado-" + familia.getId() + "@eliminado.acompaname.invalid")));
        assertThat(despues.getTelefono(), is(nullValue()));
        assertThat(despues.isActivo(), is(false));

        // El historial de la otra parte sigue ahí
        SolicitudEntity solicitudDespues = solicitudRepository.findById(solicitud.getId()).orElseThrow();
        assertThat(solicitudDespues.getFamilia().getId(), is(equalTo(familia.getId())));
        assertThat(mensajeRepository.findById(mensaje.getId()).isPresent(), is(true));
    }

    @Test
    void anonimizarBorraLasSugerenciasYLosTokensDeRecuperacion() {
        anonimizador.anonimizar(usuarioRepository.findById(familia.getId()).orElseThrow());

        boolean quedanSugerencias = sugerenciaRepository.findAll().stream()
                .anyMatch(s -> s.getUsuario() != null && s.getUsuario().getId().equals(familia.getId()));
        assertThat(quedanSugerencias, is(false));
        assertThat(tokenRepository.findByTokenHash("a".repeat(64)).isPresent(), is(false));
    }

    @Test
    void anonimizarUnCuidadorVaciaSuPerfilYLoQuitaDeLaListaPublica() {
        List<Long> antes = perfilRepository.findAllConUsuario().stream().map(PerfilCuidadorEntity::getId).toList();
        assertThat(antes, hasItem(perfil.getId()));

        anonimizador.anonimizar(usuarioRepository.findById(cuidador.getId()).orElseThrow());

        PerfilCuidadorEntity despues = perfilRepository.findById(perfil.getId()).orElseThrow();
        assertThat(despues.getFoto(), is(nullValue()));
        assertThat(despues.getBio(), is(equalTo("")));
        assertThat(despues.getCiudad(), is(nullValue()));
        assertThat(despues.isDisponibleAhora(), is(false));

        List<Long> enLaLista = perfilRepository.findAllConUsuario().stream().map(PerfilCuidadorEntity::getId).toList();
        assertThat(enLaLista, is(not(hasItem(perfil.getId()))));

        // La solicitud que tenía con la familia sigue apuntando a su perfil
        assertThat(solicitudRepository.findById(solicitud.getId()).orElseThrow().getCuidador().getId(),
                is(equalTo(perfil.getId())));
    }
}
