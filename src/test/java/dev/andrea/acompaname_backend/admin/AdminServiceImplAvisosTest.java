package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.EstadoPago;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de los avisos y de los cuidadores mejor valorados del panel de administración
@ExtendWith(MockitoExtension.class)
public class AdminServiceImplAvisosTest {
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ValoracionRepository valoracionRepository;

    private AdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminServiceImpl(usuarioRepository, perfilCuidadorRepository, solicitudRepository,
                pagoRepository, valoracionRepository);
    }

    private PerfilCuidadorEntity perfil(Long id, String nombre) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(nombre);
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setId(id);
        perfil.setUsuario(usuario);
        return perfil;
    }

    @Test
    void testSinNadaQueRevisarLosAvisosSalenACero() {
        AvisosDTOResponse resultado = service.avisos();

        assertThat(resultado.pendientesProximas48Horas(), is(equalTo(0L)));
        assertThat(resultado.pendientesVencidas(), is(equalTo(0L)));
        assertThat(resultado.pagosSinCobrarMasDeUnaSemana(), is(equalTo(0L)));
    }

    @Test
    void testLosAvisosCuentanLoQueHayQueRevisar() {
        LocalDate hoy = LocalDate.now();
        when(solicitudRepository.countByEstadoAndFechaCuidadoBetween(EstadoSolicitud.PENDIENTE, hoy,
                hoy.plusDays(2))).thenReturn(3L);
        when(solicitudRepository.countByEstadoAndFechaCuidadoBefore(EstadoSolicitud.PENDIENTE, hoy))
                .thenReturn(1L);
        when(pagoRepository.countByEstadoAndFechaBefore(EstadoPago.PENDIENTE, hoy.minusDays(7)))
                .thenReturn(2L);

        AvisosDTOResponse resultado = service.avisos();

        assertThat(resultado.pendientesProximas48Horas(), is(equalTo(3L)));
        assertThat(resultado.pendientesVencidas(), is(equalTo(1L)));
        assertThat(resultado.pagosSinCobrarMasDeUnaSemana(), is(equalTo(2L)));
    }

    @Test
    void testSinCuidadoresLaListaDeMejoresEstaVacia() {
        List<CuidadorValoradoDTOResponse> resultado = service.mejoresCuidadores();

        assertThat(resultado, is(empty()));
    }

    @Test
    void testDevuelveLosTresMejoresDeMayorAMenorNota() {
        when(perfilCuidadorRepository.findAll()).thenReturn(List.of(perfil(1L, "Ana"), perfil(2L, "Beto"),
                perfil(3L, "Carla"), perfil(4L, "Dani")));
        when(valoracionRepository.mediaPorCuidador(1L)).thenReturn(3.0);
        when(valoracionRepository.mediaPorCuidador(2L)).thenReturn(4.8);
        when(valoracionRepository.mediaPorCuidador(3L)).thenReturn(4.0);
        when(valoracionRepository.mediaPorCuidador(4L)).thenReturn(4.5);

        List<CuidadorValoradoDTOResponse> resultado = service.mejoresCuidadores();

        assertThat(resultado.size(), is(equalTo(3)));
        assertThat(resultado.get(0).nombre(), is(equalTo("Beto")));
        assertThat(resultado.get(1).nombre(), is(equalTo("Dani")));
        assertThat(resultado.get(2).nombre(), is(equalTo("Carla")));
    }

    @Test
    void testSeIgnoranLosCuidadoresSinValoraciones() {
        when(perfilCuidadorRepository.findAll()).thenReturn(List.of(perfil(1L, "Ana"), perfil(2L, "Beto")));
        // Un mock devuelve 0.0 si no se le dice nada; la base de datos real devuelve null sin valoraciones
        when(valoracionRepository.mediaPorCuidador(1L)).thenReturn(null);
        when(valoracionRepository.mediaPorCuidador(2L)).thenReturn(4.0);

        List<CuidadorValoradoDTOResponse> resultado = service.mejoresCuidadores();

        assertThat(resultado.size(), is(equalTo(1)));
        assertThat(resultado.get(0).nombre(), is(equalTo("Beto")));
    }

    @Test
    void testIncluyeLaNotaRedondeadaYLosServiciosCompletados() {
        when(perfilCuidadorRepository.findAll()).thenReturn(List.of(perfil(1L, "Ana")));
        when(valoracionRepository.mediaPorCuidador(1L)).thenReturn(4.26);
        when(solicitudRepository.countByCuidadorIdAndEstado(1L, EstadoSolicitud.COMPLETADA)).thenReturn(5L);

        List<CuidadorValoradoDTOResponse> resultado = service.mejoresCuidadores();

        assertThat(resultado.get(0).notaMedia(), is(equalTo(4.3)));
        assertThat(resultado.get(0).serviciosCompletados(), is(equalTo(5L)));
    }
}