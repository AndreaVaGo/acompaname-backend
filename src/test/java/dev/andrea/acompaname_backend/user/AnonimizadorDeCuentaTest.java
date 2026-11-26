package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.auth.CodigoLoginRepository;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.schedule.SuscripcionCalendarioRepository;
import dev.andrea.acompaname_backend.suggestion.SugerenciaRepository;
import dev.andrea.acompaname_backend.verificacion.VerificacionRepository;

// Anonimizar una cuenta: se borran sus datos personales, pero la fila se queda
// para que el historial de la otra parte (solicitudes, pagos...) no se rompa.
@ExtendWith(MockitoExtension.class)
public class AnonimizadorDeCuentaTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private CodigoLoginRepository codigoLoginRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private SugerenciaRepository sugerenciaRepository;
    @Mock
    private VerificacionRepository verificacionRepository;
    @Mock
    private BloqueOcupadoRepository bloqueOcupadoRepository;
    @Mock
    private SuscripcionCalendarioRepository suscripcionCalendarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AnonimizadorDeCuenta anonimizador;

    @BeforeEach
    void setUp() {
        anonimizador = new AnonimizadorDeCuenta(usuarioRepository, perfilCuidadorRepository, codigoLoginRepository,
                tokenRepository, sugerenciaRepository, verificacionRepository, bloqueOcupadoRepository,
                suscripcionCalendarioRepository, passwordEncoder);
    }

    private UsuarioEntity ana() {
        return new UsuarioEntity(7L, "Ana", "ana@test.com", "600111222", "hash-viejo", Set.of(), true, true);
    }

    private PerfilCuidadorEntity perfilDeAna(UsuarioEntity usuario) {
        return PerfilCuidadorEntity.builder()
                .id(3L)
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Llevo años cuidando mayores")
                .disponibleAhora(true)
                .usuario(usuario)
                .foto("data:image/png;base64,AAAA")
                .ciudad("Oviedo")
                .sexo("MUJER")
                .titulaciones("ENFERMERIA")
                .otrasTitulaciones("Curso de movilización")
                .build();
    }

    @Test
    void anonimizaLosDatosPersonalesYDejaLaCuentaInactiva() {
        UsuarioEntity usuario = ana();
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        assertThat(usuario.getNombre(), is(equalTo("Cuenta eliminada")));
        assertThat(usuario.getEmail(), is(equalTo("eliminado-7@eliminado.acompaname.invalid")));
        assertThat(usuario.getTelefono(), is(nullValue()));
        assertThat(usuario.getPassword(), is(equalTo("hash-nuevo")));
        assertThat(usuario.isActivo(), is(false));
        assertThat(usuario.isDosPasos(), is(false));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void laNuevaContrasenaEsAleatoriaYNoLaAnterior() {
        UsuarioEntity usuario = ana();
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        ArgumentCaptor<String> claveEnClaro = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(claveEnClaro.capture());
        assertThat(claveEnClaro.getValue(), is(not(equalTo("hash-viejo"))));
        // Un UUID: 36 caracteres que nadie conoce
        assertThat(claveEnClaro.getValue().length(), is(36));
    }

    @Test
    void borraSusCodigosTokensYSugerencias() {
        UsuarioEntity usuario = ana();
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        verify(codigoLoginRepository).deleteByUsuarioId(7L);
        verify(tokenRepository).deleteByUsuarioId(7L);
        verify(sugerenciaRepository).deleteByUsuarioId(7L);
    }

    @Test
    void sinPerfilDeCuidadorNoTocaLasVerificacionesNiElCalendario() {
        UsuarioEntity usuario = ana();
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        verify(verificacionRepository, never()).deleteByCuidadorId(anyLong());
        verify(bloqueOcupadoRepository, never()).deleteByCuidadorId(anyLong());
        verify(perfilCuidadorRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void conPerfilDeCuidadorLoVaciaYLoDejaNoDisponible() {
        UsuarioEntity usuario = ana();
        PerfilCuidadorEntity perfil = perfilDeAna(usuario);
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        assertThat(perfil.getFoto(), is(nullValue()));
        assertThat(perfil.getBio(), is(equalTo("")));
        assertThat(perfil.getCiudad(), is(nullValue()));
        assertThat(perfil.getSexo(), is(nullValue()));
        assertThat(perfil.getTitulaciones(), is(nullValue()));
        assertThat(perfil.getOtrasTitulaciones(), is(nullValue()));
        assertThat(perfil.isDisponibleAhora(), is(false));
        verify(perfilCuidadorRepository).save(perfil);
    }

    @Test
    void conPerfilDeCuidadorBorraSusVerificacionesYSuCalendario() {
        UsuarioEntity usuario = ana();
        PerfilCuidadorEntity perfil = perfilDeAna(usuario);
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        verify(verificacionRepository).deleteByCuidadorId(3L);
        verify(bloqueOcupadoRepository).deleteByCuidadorId(3L);
        verify(suscripcionCalendarioRepository).deleteByCuidadorId(3L);
    }

    @Test
    void elPerfilSeBuscaConElEmailAntesDeCambiarlo() {
        UsuarioEntity usuario = ana();
        when(perfilCuidadorRepository.findByUsuarioEmail("ana@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");

        anonimizador.anonimizar(usuario);

        // Si se buscara después, el email ya sería el inventado y no encontraría el perfil
        verify(perfilCuidadorRepository).findByUsuarioEmail("ana@test.com");
        verify(perfilCuidadorRepository, never())
                .findByUsuarioEmail("eliminado-7@eliminado.acompaname.invalid");
    }
}
