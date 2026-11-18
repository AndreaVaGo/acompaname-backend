package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.mail.CorreoException;
import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionPasswordInvalida;

@ExtendWith(MockitoExtension.class)
public class RecuperacionPasswordServiceImplTest {

    private static final String URL_FRONT = "http://localhost:5173";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ServicioDeCorreo correo;

    private RecuperacionPasswordServiceImpl service;
    private UsuarioEntity ana;

    @BeforeEach
    void setup() {
        service = new RecuperacionPasswordServiceImpl(usuarioRepository, tokenRepository, passwordEncoder, correo,
                60, URL_FRONT);
        ana = UsuarioEntity.builder().id(1L).nombre("Ana").email("ana@test.com").password("cifrada").build();
    }

    private PasswordResetTokenEntity tokenValidoDe(String token, UsuarioEntity usuario) {
        return PasswordResetTokenEntity.builder()
                .tokenHash(CodificadorDeTokens.hash(token))
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    // ---------- solicitarRestablecimiento ----------

    @Test
    void testSolicitarGuardaElHashDelTokenYNoElTokenDeVerdad() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("ana@test.com");

        ArgumentCaptor<PasswordResetTokenEntity> guardado = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(guardado.capture());
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(anyString(), anyString(), texto.capture());

        // El token de verdad va solo en el correo, y lo guardado es su hash
        String enlace = texto.getValue().split("token=")[1].split("\\s")[0];
        assertThat(guardado.getValue().getTokenHash(), is(equalTo(CodificadorDeTokens.hash(enlace))));
        assertThat(guardado.getValue().getTokenHash(), is(not(equalTo(enlace))));
        assertThat(guardado.getValue().getUsuario(), is(equalTo(ana)));
    }

    @Test
    void testSolicitarPoneLaCaducidadEnLosMinutosConfigurados() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));
        LocalDateTime antes = LocalDateTime.now();

        service.solicitarRestablecimiento("ana@test.com");

        ArgumentCaptor<PasswordResetTokenEntity> guardado = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(guardado.capture());
        LocalDateTime caducidad = guardado.getValue().getExpiraEn();
        assertThat(caducidad.isAfter(antes.plusMinutes(59)), is(true));
        assertThat(caducidad.isBefore(LocalDateTime.now().plusMinutes(61)), is(true));
    }

    @Test
    void testSolicitarEnviaElCorreoConElEnlaceAlFront() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("ana@test.com");

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(destino.capture(), asunto.capture(), texto.capture());
        assertThat(destino.getValue(), is(equalTo("ana@test.com")));
        assertThat(asunto.getValue(), containsString("contraseña"));
        assertThat(texto.getValue(), containsString("Hola Ana"));
        assertThat(texto.getValue(), containsString(URL_FRONT + "/restablecer-password?token="));
        assertThat(texto.getValue(), containsString("caduca en 60 minutos"));
    }

    @Test
    void testSolicitarBorraLosEnlacesAnterioresDeEsaPersona() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("ana@test.com");

        verify(tokenRepository).deleteByUsuarioId(1L);
    }

    @Test
    void testSolicitarIgnoraLosEspaciosDelCorreo() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("  ana@test.com ");

        verify(correo).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void testSolicitarConUnCorreoQueNoExisteNoHaceNadaNiFalla() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.solicitarRestablecimiento("nadie@test.com"));

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(correo);
    }

    @Test
    void testSolicitarConUnaCuentaBloqueadaNoHaceNada() {
        ana.setActivo(false);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("ana@test.com");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(correo);
    }

    @Test
    void testSolicitarSinCorreoNoHaceNada() {
        assertDoesNotThrow(() -> service.solicitarRestablecimiento(null));

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(correo);
    }

    @Test
    void testSiFallaElEnvioDelCorreoNoSeCuentaAQuienLoPidio() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));
        doThrow(new CorreoException("fallo", new RuntimeException())).when(correo).enviar(anyString(), anyString(),
                anyString());

        assertDoesNotThrow(() -> service.solicitarRestablecimiento("ana@test.com"));
    }

    @Test
    void testCadaPeticionGeneraUnTokenDistinto() {
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(ana));

        service.solicitarRestablecimiento("ana@test.com");
        service.solicitarRestablecimiento("ana@test.com");

        ArgumentCaptor<PasswordResetTokenEntity> guardados = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository, org.mockito.Mockito.times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues().get(0).getTokenHash(),
                is(not(equalTo(guardados.getAllValues().get(1).getTokenHash()))));
    }

    // ---------- restablecer ----------

    @Test
    void testRestablecerCambiaLaContrasenaConUnTokenValido() {
        when(tokenRepository.findByTokenHash(CodificadorDeTokens.hash("token-bueno")))
                .thenReturn(Optional.of(tokenValidoDe("token-bueno", ana)));
        when(passwordEncoder.matches("Nueva1234!", "cifrada")).thenReturn(false);
        when(passwordEncoder.encode("Nueva1234!")).thenReturn("nueva-cifrada");

        service.restablecer("token-bueno", "Nueva1234!");

        ArgumentCaptor<UsuarioEntity> guardado = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getPassword(), is(equalTo("nueva-cifrada")));
    }

    @Test
    void testRestablecerBorraLosTokensParaQueNoSePuedaUsarDosVeces() {
        when(tokenRepository.findByTokenHash(CodificadorDeTokens.hash("token-bueno")))
                .thenReturn(Optional.of(tokenValidoDe("token-bueno", ana)));
        when(passwordEncoder.matches("Nueva1234!", "cifrada")).thenReturn(false);
        when(passwordEncoder.encode("Nueva1234!")).thenReturn("nueva-cifrada");

        service.restablecer("token-bueno", "Nueva1234!");

        verify(tokenRepository).deleteByUsuarioId(1L);
    }

    @Test
    void testRestablecerConUnTokenQueNoExisteFalla() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.restablecer("falso", "Nueva1234!"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testRestablecerConUnTokenCaducadoFalla() {
        PasswordResetTokenEntity caducado = tokenValidoDe("viejo", ana);
        caducado.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHash(CodificadorDeTokens.hash("viejo"))).thenReturn(Optional.of(caducado));

        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.restablecer("viejo", "Nueva1234!"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testRestablecerConUnaCuentaBloqueadaFalla() {
        ana.setActivo(false);
        when(tokenRepository.findByTokenHash(CodificadorDeTokens.hash("token-bueno")))
                .thenReturn(Optional.of(tokenValidoDe("token-bueno", ana)));

        assertThrows(TokenRecuperacionInvalidoException.class,
                () -> service.restablecer("token-bueno", "Nueva1234!"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testRestablecerConLaMismaContrasenaFalla() {
        when(tokenRepository.findByTokenHash(CodificadorDeTokens.hash("token-bueno")))
                .thenReturn(Optional.of(tokenValidoDe("token-bueno", ana)));
        when(passwordEncoder.matches("Igual1234!", "cifrada")).thenReturn(true);

        UsuarioExceptionPasswordInvalida error = assertThrows(UsuarioExceptionPasswordInvalida.class,
                () -> service.restablecer("token-bueno", "Igual1234!"));

        assertThat(error.getMessage(), containsString("distinta"));
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).deleteByUsuarioId(any());
    }

    @Test
    void testRestablecerSinTokenFalla() {
        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.restablecer(null, "Nueva1234!"));
        assertThrows(TokenRecuperacionInvalidoException.class, () -> service.restablecer("   ", "Nueva1234!"));

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void testElErrorDelTokenSiempreDiceLoMismo() {
        TokenRecuperacionInvalidoException error = new TokenRecuperacionInvalidoException();

        assertThat(error.getMessage(), is(equalTo("El enlace no es válido o ha caducado. Pide uno nuevo.")));
    }
}
