package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la búsqueda de cuidadores ocupados en unas fechas (para filtrar en la búsqueda de la familia)
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplOcupadosTest {
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;

    private SolicitudServiceImpl service;

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository);
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private UsuarioEntity usuario(Long id, String nombre, String email, RoleEntity rol) {
        return new UsuarioEntity(id, nombre, email, "600111222", "1234", Set.of(rol));
    }

    // Cuidador Pepe, con perfil de id 1
    private PerfilCuidadorEntity perfilPepe() {
        UsuarioEntity pepe = usuario(2L, "Pepe", "pepe@test.com", rol(2L, "CUIDADOR"));
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, pepe);
    }

    // Cuidadora Marta, con perfil de id 2
    private PerfilCuidadorEntity perfilMarta() {
        UsuarioEntity marta = usuario(4L, "Marta", "marta@test.com", rol(2L, "CUIDADOR"));
        return new PerfilCuidadorEntity(2L, "Enfermería", 6, new BigDecimal("20.00"), "Bio", false, true, marta);
    }

    // Solicitud de la familia de Ana a ese cuidador, con las fechas y el estado que yo elija
    private SolicitudEntity solicitud(Long id, PerfilCuidadorEntity cuidador, LocalDate inicio, LocalDate fin,
            EstadoSolicitud estado) {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com", rol(1L, "FAMILIA"));
        return new SolicitudEntity(id, "Acompañamiento", "Manuel", "Sin notas", 80, inicio, fin, estado, ana,
                cuidador);
    }

    @Test
    void testUnCuidadorConServicioAceptadoEnEsasFechasEstaOcupado() {
        SolicitudEntity deMarta = solicitud(1L, perfilMarta(), LocalDate.of(2026, 9, 20), null,
                EstadoSolicitud.ACEPTADA);
        SolicitudEntity dePepe = solicitud(2L, perfilPepe(), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 12),
                EstadoSolicitud.ACEPTADA);
        when(repository.findAll()).thenReturn(List.of(deMarta, dePepe));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testSinFechaFinSeBuscaUnSoloDia() {
        SolicitudEntity dePepe = solicitud(1L, perfilPepe(), LocalDate.of(2026, 9, 10), null,
                EstadoSolicitud.ACEPTADA);
        when(repository.findAll()).thenReturn(List.of(dePepe));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null);

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testUnCuidadorConServicioEnOtrasFechasNoEstaOcupado() {
        SolicitudEntity dePepe = solicitud(1L, perfilPepe(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                EstadoSolicitud.ACEPTADA);
        when(repository.findAll()).thenReturn(List.of(dePepe));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testUnaSolicitudSoloPendienteNoOcupaAlCuidador() {
        SolicitudEntity dePepe = solicitud(1L, perfilPepe(), LocalDate.of(2026, 9, 10), null,
                EstadoSolicitud.PENDIENTE);
        when(repository.findAll()).thenReturn(List.of(dePepe));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null);

        assertThat(ocupados, is(empty()));
    }

    @Test
    void testUnCuidadorConDosServiciosCruzadosApareceUnaSolaVez() {
        PerfilCuidadorEntity pepe = perfilPepe();
        SolicitudEntity primera = solicitud(1L, pepe, LocalDate.of(2026, 9, 10), null, EstadoSolicitud.ACEPTADA);
        SolicitudEntity segunda = solicitud(2L, pepe, LocalDate.of(2026, 9, 11), null, EstadoSolicitud.ACEPTADA);
        when(repository.findAll()).thenReturn(List.of(primera, segunda));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testAceptarElDiaEnQueTerminaOtroServicioCuentaComoOcupado() {
        SolicitudEntity dePepe = solicitud(1L, perfilPepe(), LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 10),
                EstadoSolicitud.ACEPTADA);
        when(repository.findAll()).thenReturn(List.of(dePepe));

        List<Long> ocupados = service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        assertThat(ocupados, is(equalTo(List.of(1L))));
    }

    @Test
    void testConFechaFinAnteriorALaDeInicioDaFechaInvalida() {
        assertThrows(SolicitudExceptionFechaInvalida.class,
                () -> service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 5)));
    }
}