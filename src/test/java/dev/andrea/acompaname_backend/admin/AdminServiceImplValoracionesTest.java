package dev.andrea.acompaname_backend.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.admin.dtos.AdminValoracionDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.review.ValoracionEntity;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.review.exceptions.ValoracionExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de las valoraciones en el panel de administración: ver las últimas y borrar una
@ExtendWith(MockitoExtension.class)
public class AdminServiceImplValoracionesTest {
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

    // Valoración de la familia al cuidador, con los nombres que se ven en el panel
    private ValoracionEntity valoracion(Long id, int puntuacion, String nombreFamilia, String nombreCuidador) {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setNombre(nombreFamilia);
        UsuarioEntity usuarioCuidador = new UsuarioEntity();
        usuarioCuidador.setNombre(nombreCuidador);
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity();
        cuidador.setUsuario(usuarioCuidador);

        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);

        ValoracionEntity valoracion = new ValoracionEntity();
        valoracion.setId(id);
        valoracion.setPuntuacion(puntuacion);
        valoracion.setComentario("Comentario " + id);
        valoracion.setFecha(LocalDate.of(2026, 11, 10));
        valoracion.setSolicitud(solicitud);
        return valoracion;
    }

    @Test
    void testSinValoracionesLaListaDeUltimasEstaVacia() {
        List<AdminValoracionDTOResponse> resultado = service.ultimasValoraciones();

        assertThat(resultado, is(empty()));
    }

    @Test
    void testLasUltimasValoracionesIncluyenLosNombresDeLaFamiliaYDelCuidador() {
        when(valoracionRepository.findTop5ByOrderByFechaDescIdDesc())
                .thenReturn(List.of(valoracion(2L, 5, "Ana", "Pepe"), valoracion(1L, 3, "Luis", "Marta")));

        List<AdminValoracionDTOResponse> resultado = service.ultimasValoraciones();

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).id(), is(equalTo(2L)));
        assertThat(resultado.get(0).puntuacion(), is(equalTo(5)));
        assertThat(resultado.get(0).comentario(), is(equalTo("Comentario 2")));
        assertThat(resultado.get(0).nombreFamilia(), is(equalTo("Ana")));
        assertThat(resultado.get(0).nombreCuidador(), is(equalTo("Pepe")));
        assertThat(resultado.get(1).nombreFamilia(), is(equalTo("Luis")));
        assertThat(resultado.get(1).nombreCuidador(), is(equalTo("Marta")));
    }

    @Test
    void testBorrarUnaValoracionQueExisteLaBorra() {
        when(valoracionRepository.existsById(1L)).thenReturn(true);

        service.borrarValoracion(1L);

        verify(valoracionRepository).deleteById(1L);
    }

    @Test
    void testBorrarUnaValoracionQueNoExisteDaNotFoundYNoBorraNada() {
        when(valoracionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValoracionExceptionNotFound.class, () -> service.borrarValoracion(99L));

        verify(valoracionRepository, never()).deleteById(99L);
    }
}