package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Comprueba que la lista de cuidadores no hace una consulta por cada cuidador (problema N+1)
// y que la valoración media y el número de valoraciones salen bien para cada uno.
@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplListaTest {

    @Mock
    private PerfilCuidadorRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ValoracionRepository valoracionRepository;
    @Mock
    private FotoValidador fotoValidador;

    private PerfilCuidadorServiceImpl service;

    @BeforeEach
    void setup() {
        service = new PerfilCuidadorServiceImpl(repository, usuarioRepository, valoracionRepository, fotoValidador);
    }

    private PerfilCuidadorEntity crearPerfil(Long id, String nombre) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id + 100);
        usuario.setNombre(nombre);
        return new PerfilCuidadorEntity(id, "Geriatría", 4, new BigDecimal("18.00"), "Bio", false, false, usuario);
    }

    // Una fila del resumen es: [id del cuidador, media, número de valoraciones]
    private Object[] fila(Long cuidadorId, Double media, Long numero) {
        return new Object[] { cuidadorId, media, numero };
    }

    @Test
    void testCadaCuidadorRecibeSuMediaYSuNumeroDeValoraciones() {
        when(repository.findAllConUsuario())
                .thenReturn(List.of(crearPerfil(1L, "Pepe"), crearPerfil(2L, "Marta")));
        List<Object[]> resumen = new ArrayList<>();
        resumen.add(fila(1L, 4.0, 3L));
        resumen.add(fila(2L, 2.5, 2L));
        when(valoracionRepository.resumenPorCuidador()).thenReturn(resumen);

        List<PerfilCuidadorDTOResponse> lista = service.getEntities();

        assertThat(lista.size(), is(equalTo(2)));
        assertThat(lista.get(0).valoracionMedia(), is(equalTo(4.0)));
        assertThat(lista.get(0).numValoraciones(), is(equalTo(3L)));
        assertThat(lista.get(1).valoracionMedia(), is(equalTo(2.5)));
        assertThat(lista.get(1).numValoraciones(), is(equalTo(2L)));
    }

    @Test
    void testUnCuidadorSinValoracionesTieneMediaNulaYCeroValoraciones() {
        when(repository.findAllConUsuario())
                .thenReturn(List.of(crearPerfil(1L, "Pepe"), crearPerfil(2L, "Marta")));
        List<Object[]> resumen = new ArrayList<>();
        resumen.add(fila(1L, 5.0, 1L));
        when(valoracionRepository.resumenPorCuidador()).thenReturn(resumen);

        List<PerfilCuidadorDTOResponse> lista = service.getEntities();

        assertThat(lista.get(0).valoracionMedia(), is(equalTo(5.0)));
        assertThat(lista.get(1).valoracionMedia(), is(nullValue()));
        assertThat(lista.get(1).numValoraciones(), is(equalTo(0L)));
    }

    @Test
    void testLaMediaSeRedondeaAUnDecimal() {
        when(repository.findAllConUsuario()).thenReturn(List.of(crearPerfil(1L, "Pepe")));
        List<Object[]> resumen = new ArrayList<>();
        resumen.add(fila(1L, 4.3333333, 3L));
        when(valoracionRepository.resumenPorCuidador()).thenReturn(resumen);

        List<PerfilCuidadorDTOResponse> lista = service.getEntities();

        assertThat(lista.get(0).valoracionMedia(), is(equalTo(4.3)));
    }

    @Test
    void testSinCuidadoresDevuelveListaVacia() {
        when(repository.findAllConUsuario()).thenReturn(new ArrayList<>());

        List<PerfilCuidadorDTOResponse> lista = service.getEntities();

        assertThat(lista.isEmpty(), is(true));
    }

    @Test
    void testLaListaNoHaceUnaConsultaPorCadaCuidador() {
        when(repository.findAllConUsuario()).thenReturn(List.of(
                crearPerfil(1L, "Pepe"), crearPerfil(2L, "Marta"), crearPerfil(3L, "Luis"),
                crearPerfil(4L, "Ana"), crearPerfil(5L, "Rosa")));
        when(valoracionRepository.resumenPorCuidador()).thenReturn(new ArrayList<>());

        service.getEntities();

        // Una consulta para los perfiles y una para las valoraciones, aunque haya 5 cuidadores
        verify(repository, times(1)).findAllConUsuario();
        verify(valoracionRepository, times(1)).resumenPorCuidador();
        // Y nunca las consultas de una en una
        verify(repository, never()).findAll();
        verify(valoracionRepository, never()).mediaPorCuidador(anyLong());
        verify(valoracionRepository, never()).countBySolicitudCuidadorId(anyLong());
    }
}
