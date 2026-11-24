package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// La foto de los perfiles: en la lista y en el perfil público va como dirección, en "mi perfil" como se guardó
// (para poder editarla) y se puede pedir aparte como imagen (ACOM-158)
@ExtendWith(MockitoExtension.class)
public class PerfilCuidadorServiceImplFotoTest {

    private static final String FOTO = "data:image/jpeg;base64,AQIDBA==";

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

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private PerfilCuidadorEntity perfil(Long id, String foto) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id + 100);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(id, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                false, false, usuario);
        perfil.setFoto(foto);
        return perfil;
    }

    @Test
    void testLaListaLlevaLaDireccionDeLaFotoYNoLaImagen() {
        when(repository.findAllConUsuario()).thenReturn(List.of(perfil(1L, FOTO)));

        List<PerfilCuidadorDTOResponse> lista = service.getEntities();

        assertThat(lista.get(0).foto(), startsWith("/cuidadores/1/foto?v="));
        assertThat(lista.get(0).foto(), not(containsString("base64")));
    }

    @Test
    void testElPerfilPublicoLlevaLaDireccionDeLaFoto() {
        when(repository.findById(1L)).thenReturn(Optional.of(perfil(1L, FOTO)));

        PerfilCuidadorDTOResponse dto = service.getById(1L);

        assertThat(dto.foto(), startsWith("/cuidadores/1/foto?v="));
    }

    @Test
    void testMiPerfilDevuelveLaFotoComoSeGuardoParaPoderEditarla() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("pepe@test.com", null));
        when(repository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(perfil(1L, FOTO)));

        PerfilCuidadorDTOResponse dto = service.getMiPerfil();

        assertThat(dto.foto(), is(equalTo(FOTO)));
    }

    @Test
    void testPedirLaFotoDevuelveSuTipoYSusBytes() {
        when(repository.findById(1L)).thenReturn(Optional.of(perfil(1L, FOTO)));

        FotoDescargable foto = service.getFoto(1L);

        assertThat(foto.tipo(), is(equalTo("image/jpeg")));
        assertThat(foto.bytes(), is(equalTo(new byte[] { 1, 2, 3, 4 })));
    }

    @Test
    void testPedirLaFotoDeUnPerfilSinFotoDaNoEncontrado() {
        when(repository.findById(1L)).thenReturn(Optional.of(perfil(1L, null)));

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.getFoto(1L));
    }

    @Test
    void testPedirLaFotoDeUnPerfilQueNoExisteDaNoEncontrado() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.getFoto(9L));
    }

    @Test
    void testUnaFotoQueEsUnaDireccionExternaNoSePuedePedirComoImagen() {
        when(repository.findById(1L)).thenReturn(Optional.of(perfil(1L, "https://ejemplo.com/foto.jpg")));

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service.getFoto(1L));
    }
}
