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
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la validación de las horas al crear y editar una solicitud
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplHorasTest {
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

    // Petición con las fechas y horas que le pasemos
    private SolicitudDTORequest dto(LocalDate fechaFin, LocalTime inicio, LocalTime fin) {
        return new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                fechaFin, 1L, 1L, null, null, null, inicio, fin);
    }

    @Test
    void testStoreUnSoloDiaConHoraDeFinIgualALaDeInicioDaErrorYNoGuarda() {
        SolicitudDTORequest dto = dto(null, LocalTime.of(9, 0), LocalTime.of(9, 0));

        assertThrows(SolicitudExceptionFechaInvalida.class, () -> service.storeEntity(dto));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreUnSoloDiaConHoraDeFinAnteriorDaErrorYNoGuarda() {
        SolicitudDTORequest dto = dto(null, LocalTime.of(12, 0), LocalTime.of(9, 0));

        assertThrows(SolicitudExceptionFechaInvalida.class, () -> service.storeEntity(dto));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConFechaFinIgualALaDeInicioYHorasInvertidasDaError() {
        SolicitudDTORequest dto = dto(LocalDate.of(2026, 9, 10), LocalTime.of(18, 0), LocalTime.of(15, 0));

        assertThrows(SolicitudExceptionFechaInvalida.class, () -> service.storeEntity(dto));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testUpdateUnSoloDiaConHorasInvertidasDaErrorYNoGuarda() {
        SolicitudDTORequest dto = dto(null, LocalTime.of(12, 0), LocalTime.of(9, 0));

        assertThrows(SolicitudExceptionFechaInvalida.class, () -> service.update(1L, dto));
        verify(repository, never()).save(Mockito.any(SolicitudEntity.class));
    }

    @Test
    void testStoreConVariosDiasPermiteQueLaHoraDeFinSeaAnteriorALaDeInicio() {
        // Del 10 a las 22:00 al 12 a las 08:00: la hora de fin es el último día, así que es válida
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudDTOResponse respuesta = service
                .storeEntity(dto(LocalDate.of(2026, 9, 12), LocalTime.of(22, 0), LocalTime.of(8, 0)));

        assertThat(respuesta.horaInicio(), is(equalTo(LocalTime.of(22, 0))));
        assertThat(respuesta.horaFin(), is(equalTo(LocalTime.of(8, 0))));
    }

    @Test
    void testStoreUnSoloDiaConHorasCorrectasGuardaLasHoras() {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(perfilPepe()));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudDTOResponse respuesta = service.storeEntity(dto(null, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        assertThat(respuesta.horaInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(respuesta.horaFin(), is(equalTo(LocalTime.of(12, 0))));
    }

    @Test
    void testUpdateCambiaLasHoras() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = solicitudPendiente();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.update(1L, dto(null, LocalTime.of(15, 0), LocalTime.of(18, 0)));

        assertThat(solicitud.getHoraInicio(), is(equalTo(LocalTime.of(15, 0))));
        assertThat(solicitud.getHoraFin(), is(equalTo(LocalTime.of(18, 0))));
    }
}
