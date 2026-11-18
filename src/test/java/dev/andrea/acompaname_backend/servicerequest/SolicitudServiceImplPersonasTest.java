package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
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
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionPersonasInvalidas;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la lista de personas a cuidar al crear y editar una solicitud
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplPersonasTest {
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

    private PerfilCuidadorEntity perfilPepe() {
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe);
    }

    private SolicitudEntity solicitudPendiente() {
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.PENDIENTE, ana(), perfilPepe());
    }

    private List<PersonaCuidadaDTO> lista(int cuantas) {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        for (int i = 1; i <= cuantas; i++) {
            personas.add(new PersonaCuidadaDTO("Persona " + i, 10 + i));
        }
        return personas;
    }

    private SolicitudDTORequest dto(String nombre, Integer edad, List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("Acompañamiento", nombre, "Sin notas", edad, LocalDate.of(2026, 9, 10), null,
                1L, 1L, null, null, null, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    private void prepararStore() {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void testStoreConTresPersonasLasGuardaYLasDevuelve() {
        prepararStore();

        SolicitudDTOResponse respuesta = service.storeEntity(dto(null, null, lista(3)));

        assertThat(respuesta.personas().size(), is(equalTo(3)));
        assertThat(respuesta.personas().get(2).nombre(), is(equalTo("Persona 3")));
        assertThat(respuesta.nombrePaciente(), is(equalTo("Persona 1")));
        assertThat(respuesta.edadPaciente(), is(equalTo(11)));
    }

    @Test
    void testStoreSinPersonasDaErrorYNoGuarda() {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));

        assertThrows(SolicitudExceptionPersonasInvalidas.class, () -> service.storeEntity(dto(null, null, null)));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConMasDeCincoPersonasDaErrorYNoGuarda() {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));

        assertThrows(SolicitudExceptionPersonasInvalidas.class, () -> service.storeEntity(dto(null, null, lista(6))));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConLaFormaAntiguaGuardaUnaPersona() {
        prepararStore();

        SolicitudDTOResponse respuesta = service.storeEntity(dto("Manuel", 80, null));

        assertThat(respuesta.personas().size(), is(equalTo(1)));
        assertThat(respuesta.personas().get(0).nombre(), is(equalTo("Manuel")));
    }

    @Test
    void testUpdateCambiaLaListaDePersonas() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = solicitudPendiente();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.update(1L, dto(null, null, lista(4)));

        assertThat(solicitud.getPersonas().size(), is(equalTo(4)));
        assertThat(solicitud.getNombrePaciente(), is(equalTo("Persona 1")));
        assertThat(solicitud.getEdadPaciente(), is(equalTo(11)));
    }

    @Test
    void testUpdateConMasDeCincoPersonasDaErrorYNoGuarda() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudPendiente()));

        assertThrows(SolicitudExceptionPersonasInvalidas.class, () -> service.update(1L, dto(null, null, lista(6))));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testElCuidadorVeTodasLasPersonasAntesDeAceptar() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = solicitudPendiente();
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));
        solicitud.setDireccion("Calle Uría 10");
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));

        SolicitudDTOResponse respuesta = service.getById(1L);

        assertThat(respuesta.personas().size(), is(equalTo(2)));
        assertThat(respuesta.personas().get(1).nombre(), is(equalTo("Antonio")));
        // La dirección sigue oculta hasta que acepte
        assertThat(respuesta.direccion(), is(equalTo(null)));
    }
}
