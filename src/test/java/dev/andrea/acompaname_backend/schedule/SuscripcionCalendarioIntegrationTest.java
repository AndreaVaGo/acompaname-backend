package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.security.JwtService;
import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La aplicación completa con MySQL de verdad y un descargador falso (no se toca internet):
// vincular, sincronizar y desvincular, y que solo lo pueda usar una persona cuidadora.
@SpringBootTest(properties = "rate-limit.max-requests=1000")
@AutoConfigureMockMvc
public class SuscripcionCalendarioIntegrationTest extends ContenedorMySqlAplicacion {

    private static final String RUTA = "/api/v1/bloques-ocupados/suscripcion";
    private static final String URL = "https://calendar.google.com/calendar/ical/x/private-abc/basic.ics";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private BloqueOcupadoRepository bloqueRepository;
    @Autowired
    private SuscripcionCalendarioRepository suscripcionRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DescargadorDeCalendario descargador;

    private UsuarioEntity familia;
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
        familia = crearUsuario("Ana", "ana.suscr@test.com", "FAMILIA");
        pepe = crearUsuario("Pepe", "pepe.suscr@test.com", "CUIDADOR");
        perfilPepe = perfilRepository.save(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(pepe)
                .build());
        when(descargador.normalizar(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void borrarTodo() {
        suscripcionRepository.deleteAll();
        bloqueRepository.deleteAll();
        perfilRepository.delete(perfilPepe);
        usuarioRepository.delete(familia);
        usuarioRepository.delete(pepe);
    }

    private String token(UsuarioEntity usuario) {
        return jwtService.crearToken(usuario);
    }

    private String calendario(LocalDate dia, String desde, String hasta) {
        String f = dia.toString().replace("-", "");
        return "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Cita privada\r\nDTSTART:" + f + "T" + desde
                + "\r\nDTEND:" + f + "T" + hasta + "\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
    }

    private void vincular() throws Exception {
        mockMvc.perform(put(RUTA)
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + URL + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testVincularCreaLosBloquesYNoDevuelveLaDireccion() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(5);
        when(descargador.descargar(URL)).thenReturn(calendario(dia, "090000", "110000"));

        mockMvc.perform(put(RUTA)
                .header("Authorization", "Bearer " + token(pepe))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + URL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(true))
                .andExpect(jsonPath("$.url").doesNotExist());

        List<BloqueOcupadoEntity> bloques = bloqueRepository
                .findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfilPepe.getId());
        assertThat(bloques.size(), is(1));
        assertThat(bloques.get(0).getMotivo(), is(equalTo("Calendario sincronizado")));
        assertThat(bloques.get(0).getHoraInicio(), is(equalTo(LocalTime.of(9, 0))));
    }

    @Test
    void testSincronizarCambiaLosBloquesSincronizadosPeroNoLosDeLaPersona() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(5);
        when(descargador.descargar(URL)).thenReturn(calendario(dia, "090000", "110000"));
        vincular();
        bloqueRepository.save(BloqueOcupadoEntity.builder().cuidador(perfilPepe).fecha(dia.plusDays(1))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(15, 0)).motivo("Mi turno").build());

        // En el calendario externo el evento ha cambiado de hora
        when(descargador.descargar(URL)).thenReturn(calendario(dia, "160000", "180000"));
        mockMvc.perform(post(RUTA + "/sincronizar")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoError").doesNotExist());

        List<BloqueOcupadoEntity> bloques = bloqueRepository
                .findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfilPepe.getId());
        assertThat(bloques.size(), is(2));
        assertThat(bloques.get(0).getHoraInicio(), is(equalTo(LocalTime.of(16, 0))));
        assertThat(bloques.get(1).getMotivo(), is(equalTo("Mi turno")));
    }

    @Test
    void testSiLaSincronizacionFallaSeQuedanLosBloquesYSeApuntaElError() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(5);
        when(descargador.descargar(URL)).thenReturn(calendario(dia, "090000", "110000"));
        vincular();

        when(descargador.descargar(URL)).thenThrow(new BloqueOcupadoExceptionFechaInvalida("No se pudo descargar"));
        mockMvc.perform(post(RUTA + "/sincronizar")
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoError").value("No se pudo descargar"));

        assertThat(bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfilPepe.getId()).size(), is(1));
    }

    @Test
    void testDesvincularBorraLaSuscripcionYSusBloquesPeroNoLosDeLaPersona() throws Exception {
        LocalDate dia = LocalDate.now().plusDays(5);
        when(descargador.descargar(URL)).thenReturn(calendario(dia, "090000", "110000"));
        vincular();
        bloqueRepository.save(BloqueOcupadoEntity.builder().cuidador(perfilPepe).fecha(dia.plusDays(1))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(15, 0)).motivo("Mi turno").build());

        mockMvc.perform(delete(RUTA)
                .header("Authorization", "Bearer " + token(pepe)))
                .andExpect(status().isNoContent());

        List<BloqueOcupadoEntity> bloques = bloqueRepository
                .findByCuidadorIdOrderByFechaAscHoraInicioAsc(perfilPepe.getId());
        assertThat(bloques.size(), is(1));
        assertThat(bloques.get(0).getMotivo(), is(equalTo("Mi turno")));
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + token(pepe)))
                .andExpect(jsonPath("$.vinculado").value(false));
    }

    @Test
    void testVincularDosVecesSigueSiendoUnaSolaSuscripcion() throws Exception {
        when(descargador.descargar(URL)).thenReturn(calendario(LocalDate.now().plusDays(5), "090000", "110000"));
        vincular();
        vincular();

        assertThat(suscripcionRepository.count(), is(1L));
    }

    @Test
    void testUnaFamiliaNoPuedeUsarLaVinculacion() throws Exception {
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + token(familia)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(RUTA)
                .header("Authorization", "Bearer " + token(familia))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + URL + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSinSesionDaNoAutorizado() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }
}
