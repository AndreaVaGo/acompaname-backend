package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.generics.PaginaDTOResponse;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la lista de solicitudes por páginas. El orden y el filtro por usuario los hace la base de datos
// (se prueban con MySQL real en SolicitudRepositoryIntegrationTest); aquí, lo que decide el servicio.
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplPaginaTest {
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

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                pagoRepository, bloqueRepository);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("ana@test.com", null));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private SolicitudEntity solicitud(Long id) {
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rol(1L, "FAMILIA")));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                new java.math.BigDecimal("18.00"), "Bio", true, true,
                new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rol(2L, "CUIDADOR"))));
        return new SolicitudEntity(id, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
    }

    private Pageable pageableUsado() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByParticipante(eq("ana@test.com"), captor.capture());
        return captor.getValue();
    }

    @Test
    void testDevuelveLaPaginaConSusDatos() {
        Page<SolicitudEntity> pagina = new PageImpl<>(List.of(solicitud(1L), solicitud(2L)),
                PageRequest.of(1, 2), 5);
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class))).thenReturn(pagina);

        PaginaDTOResponse<SolicitudDTOResponse> resultado = service.getMisSolicitudesPaginadas(1, 2);

        assertThat(resultado.contenido().size(), is(equalTo(2)));
        assertThat(resultado.pagina(), is(equalTo(1)));
        assertThat(resultado.tamano(), is(equalTo(2)));
        assertThat(resultado.totalElementos(), is(equalTo(5L)));
        assertThat(resultado.totalPaginas(), is(equalTo(3)));
    }

    @Test
    void testPideAlRepositorioLaPaginaYElTamanoIndicados() {
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getMisSolicitudesPaginadas(3, 10);

        Pageable pageable = pageableUsado();
        assertThat(pageable.getPageNumber(), is(equalTo(3)));
        assertThat(pageable.getPageSize(), is(equalTo(10)));
    }

    @Test
    void testOrdenaLasMasRecientesPrimero() {
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getMisSolicitudesPaginadas(0, 10);

        Pageable pageable = pageableUsado();
        assertThat(pageable.getSort().getOrderFor("fechaCuidado").isDescending(), is(true));
        assertThat(pageable.getSort().getOrderFor("id").isDescending(), is(true));
    }

    @Test
    void testElTamanoTieneUnMaximo() {
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getMisSolicitudesPaginadas(0, 100000);

        assertThat(pageableUsado().getPageSize(), is(equalTo(SolicitudServiceImpl.TAMANO_MAXIMO_PAGINA)));
    }

    @Test
    void testUnTamanoCeroONegativoSeConvierteEnUno() {
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getMisSolicitudesPaginadas(0, 0);

        assertThat(pageableUsado().getPageSize(), is(equalTo(1)));
    }

    @Test
    void testUnaPaginaNegativaCuentaComoLaPrimera() {
        when(repository.findByParticipante(eq("ana@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getMisSolicitudesPaginadas(-4, 10);

        assertThat(pageableUsado().getPageNumber(), is(equalTo(0)));
    }
}
