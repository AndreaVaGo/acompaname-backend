package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionGrupoNoAtendido;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la regla: la persona cuidadora tiene que atender todos los grupos de edad de la solicitud
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplGruposAtendidosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private BloqueOcupadoRepository bloqueRepository;

    private SolicitudServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository, bloqueRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private UsuarioEntity ana() {
        return new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol(1L, "FAMILIA")));
    }

    // Cuidadora que atiende los grupos que le pasemos (null = no ha marcado ninguno)
    private PerfilCuidadorEntity perfilPepe(String grupos) {
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, pepe);
        perfil.setGruposEdad(grupos);
        return perfil;
    }

    private List<PersonaCuidadaDTO> ninosYAbuelo() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Zoe", 5, GrupoEdad.NINOS));
        personas.add(new PersonaCuidadaDTO("Abuelo", 80, GrupoEdad.MAYORES));
        return personas;
    }

    private SolicitudDTORequest peticion(List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("Acompañamiento", null, "Sin notas", null, LocalDate.of(2026, 9, 10), null, 1L,
                1L, null, null, null, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    private void prepararStore(String gruposDeLaCuidadora) {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe(gruposDeLaCuidadora)));
    }

    @Test
    void testStoreSiLaCuidadoraAtiendeTodosLosGruposGuarda() {
        prepararStore("NINOS,MAYORES");
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudDTOResponse respuesta = service.storeEntity(peticion(ninosYAbuelo()));

        assertThat(respuesta.personas().size(), is(equalTo(2)));
        assertThat(respuesta.personas().get(1).grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
        assertThat(respuesta.grupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testStoreSiFaltaUnGrupoDaErrorConSuNombreYNoGuarda() {
        prepararStore("NINOS");

        SolicitudExceptionGrupoNoAtendido error = assertThrows(SolicitudExceptionGrupoNoAtendido.class,
                () -> service.storeEntity(peticion(ninosYAbuelo())));

        assertThat(error.getMessage(), containsString("Personas mayores"));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreSiLaCuidadoraNoHaMarcadoGruposNoHayRestriccion() {
        prepararStore(null);
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudDTOResponse respuesta = service.storeEntity(peticion(ninosYAbuelo()));

        assertThat(respuesta.personas().size(), is(equalTo(2)));
    }

    @Test
    void testStoreSiLasPersonasNoTienenGrupoNoHayRestriccion() {
        prepararStore("BEBES");
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Zoe", 5));

        SolicitudDTOResponse respuesta = service.storeEntity(peticion(personas));

        assertThat(respuesta.personas().size(), is(equalTo(1)));
    }

    @Test
    void testUpdateSiFaltaUnGrupoDaErrorYNoGuarda() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, ana(), perfilPepe("NINOS"));
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));

        assertThrows(SolicitudExceptionGrupoNoAtendido.class, () -> service.update(1L, peticion(ninosYAbuelo())));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testUpdateGuardaLosGruposYElDeLaSolicitudEsElDeLaPrimeraPersona() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, ana(), perfilPepe("NINOS,MAYORES"));
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.update(1L, peticion(ninosYAbuelo()));

        assertThat(solicitud.getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
        assertThat(solicitud.getPersonas().get(1).getGrupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }
}
