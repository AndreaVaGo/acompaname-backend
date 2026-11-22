package dev.andrea.acompaname_backend.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.passwordreset.CodificadorDeTokens;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

public class DosPasosServiceImplTest {

    private CodigoLoginRepository codigoRepository;
    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private ServicioDeCorreo correo;
    private DosPasosServiceImpl service;
    private UsuarioEntity usuario;

    @BeforeEach
    void preparar() {
        codigoRepository = Mockito.mock(CodigoLoginRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        correo = Mockito.mock(ServicioDeCorreo.class);
        service = new DosPasosServiceImpl(codigoRepository, usuarioRepository, passwordEncoder, correo, 10, 3);

        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        usuario = new UsuarioEntity(5L, "Ana", "ana@test.com", "600111222", "hash-de-la-clave", Set.of(rol));
    }

    // Hace iniciar() y devuelve la entidad guardada junto con el código que se mandó por correo
    private record Inicio(String desafio, String codigo, CodigoLoginEntity entidad) {
    }

    private Inicio iniciarYCapturar() {
        String desafio = service.iniciar(usuario);

        ArgumentCaptor<CodigoLoginEntity> guardado = ArgumentCaptor.forClass(CodigoLoginEntity.class);
        Mockito.verify(codigoRepository).save(guardado.capture());
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        Mockito.verify(correo).enviar(ArgumentMatchers.eq("ana@test.com"), ArgumentMatchers.anyString(),
                texto.capture());
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6})").matcher(texto.getValue());
        assertThat(m.find(), is(true));
        return new Inicio(desafio, m.group(1), guardado.getValue());
    }

    private void simularCodigoGuardado(Inicio inicio) {
        Mockito.when(codigoRepository.findByDesafioHash(CodificadorDeTokens.hash(inicio.desafio())))
                .thenReturn(Optional.of(inicio.entidad()));
    }

    // ---------- iniciar ----------

    @Test
    void testIniciarGuardaElResumenDelCodigoYNuncaElCodigo() {
        Inicio inicio = iniciarYCapturar();

        assertThat(inicio.entidad().getDesafioHash(), is(equalTo(CodificadorDeTokens.hash(inicio.desafio()))));
        assertThat(inicio.entidad().getCodigoHash(), not(containsString(inicio.codigo())));
        assertThat(inicio.entidad().getCodigoHash().length(), is(equalTo(64)));
        assertThat(inicio.entidad().getUsuario(), is(equalTo(usuario)));
        assertThat(inicio.entidad().getIntentos(), is(equalTo(0)));
    }

    @Test
    void testIniciarMandaUnCodigoDeSeisCifrasPorCorreo() {
        Inicio inicio = iniciarYCapturar();

        assertThat(inicio.codigo(), matchesPattern("\\d{6}"));
        Mockito.verify(correo).enviar(ArgumentMatchers.eq("ana@test.com"), ArgumentMatchers.contains("código"),
                ArgumentMatchers.contains("Hola Ana"));
    }

    @Test
    void testIniciarDaUnaCaducidadDeDiezMinutos() {
        Inicio inicio = iniciarYCapturar();

        LocalDateTime expira = inicio.entidad().getExpiraEn();
        assertThat(expira.isAfter(LocalDateTime.now().plusMinutes(9)), is(true));
        assertThat(expira.isBefore(LocalDateTime.now().plusMinutes(11)), is(true));
    }

    @Test
    void testIniciarBorraLosCodigosAnterioresDeEsaPersona() {
        service.iniciar(usuario);

        Mockito.verify(codigoRepository).deleteByUsuarioId(5L);
    }

    @Test
    void testIniciarDaUnDesafioDistintoCadaVez() {
        String primero = service.iniciar(usuario);
        String segundo = service.iniciar(usuario);

        assertThat(primero, not(equalTo(segundo)));
        assertThat(primero.length(), is(greaterThan(20)));
    }

    @Test
    void testSiElCorreoFallaElErrorSube() {
        Mockito.doThrow(new RuntimeException("sin correo")).when(correo)
                .enviar(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        assertThrows(RuntimeException.class, () -> service.iniciar(usuario));
    }

    // ---------- verificar ----------

    @Test
    void testVerificarConElCodigoCorrectoDevuelveLaPersonaYGastaElCodigo() {
        Inicio inicio = iniciarYCapturar();
        simularCodigoGuardado(inicio);

        UsuarioEntity resultado = service.verificar(inicio.desafio(), inicio.codigo());

        assertThat(resultado, is(equalTo(usuario)));
        Mockito.verify(codigoRepository).delete(inicio.entidad());
    }

    @Test
    void testVerificarAceptaElCodigoConEspaciosAlPrincipioOAlFinal() {
        Inicio inicio = iniciarYCapturar();
        simularCodigoGuardado(inicio);

        UsuarioEntity resultado = service.verificar(inicio.desafio(), " " + inicio.codigo() + " ");

        assertThat(resultado, is(equalTo(usuario)));
    }

    @Test
    void testVerificarConCodigoIncorrectoSumaUnIntentoYLanzaError() {
        Inicio inicio = iniciarYCapturar();
        simularCodigoGuardado(inicio);
        String incorrecto = inicio.codigo().equals("000000") ? "111111" : "000000";

        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), incorrecto));

        assertThat(inicio.entidad().getIntentos(), is(equalTo(1)));
        Mockito.verify(codigoRepository, Mockito.times(2)).save(inicio.entidad());
        Mockito.verify(codigoRepository, Mockito.never()).delete(ArgumentMatchers.any(CodigoLoginEntity.class));
    }

    @Test
    void testAlGastarLosIntentosElCodigoSeAnulaAunqueDespuesSeEscribaElCorrecto() {
        Inicio inicio = iniciarYCapturar();
        simularCodigoGuardado(inicio);
        String incorrecto = inicio.codigo().equals("000000") ? "111111" : "000000";

        // El máximo de este test es 3 intentos
        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), incorrecto));
        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), incorrecto));
        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), incorrecto));

        Mockito.verify(codigoRepository).delete(inicio.entidad());
    }

    @Test
    void testVerificarConUnDesafioQueNoExisteLanzaError() {
        Mockito.when(codigoRepository.findByDesafioHash(ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        assertThrows(CodigoInvalidoException.class, () -> service.verificar("desconocido", "123456"));
    }

    @Test
    void testVerificarConUnCodigoCaducadoLoBorraYLanzaError() {
        Inicio inicio = iniciarYCapturar();
        inicio.entidad().setExpiraEn(LocalDateTime.now().minusMinutes(1));
        simularCodigoGuardado(inicio);

        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), inicio.codigo()));

        Mockito.verify(codigoRepository).delete(inicio.entidad());
    }

    @Test
    void testVerificarConUnaPersonaBloqueadaLanzaError() {
        Inicio inicio = iniciarYCapturar();
        usuario.setActivo(false);
        simularCodigoGuardado(inicio);

        assertThrows(CodigoInvalidoException.class, () -> service.verificar(inicio.desafio(), inicio.codigo()));
    }

    @Test
    void testVerificarSinDesafioOSinCodigoLanzaErrorSinMirarLaBaseDeDatos() {
        assertThrows(CodigoInvalidoException.class, () -> service.verificar(null, "123456"));
        assertThrows(CodigoInvalidoException.class, () -> service.verificar(" ", "123456"));
        assertThrows(CodigoInvalidoException.class, () -> service.verificar("desafio", null));
        assertThrows(CodigoInvalidoException.class, () -> service.verificar("desafio", ""));

        Mockito.verifyNoInteractions(codigoRepository);
    }

    @Test
    void testUnCodigoNoSirveConOtroDesafio() {
        Inicio inicio = iniciarYCapturar();
        // El código de verdad se pide con otro desafío: no hay nada guardado con ese resumen
        Mockito.when(codigoRepository.findByDesafioHash(CodificadorDeTokens.hash("otro-desafio")))
                .thenReturn(Optional.empty());

        assertThrows(CodigoInvalidoException.class, () -> service.verificar("otro-desafio", inicio.codigo()));
    }

    // ---------- cambiarEstado ----------

    @Test
    void testActivarConLaContrasenaCorrectaGuardaElCambio() {
        Mockito.when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        Mockito.when(passwordEncoder.matches("1234", "hash-de-la-clave")).thenReturn(true);

        service.cambiarEstado(5L, true, "1234");

        assertThat(usuario.isDosPasos(), is(true));
        Mockito.verify(usuarioRepository).save(usuario);
        Mockito.verify(codigoRepository, Mockito.never()).deleteByUsuarioId(ArgumentMatchers.anyLong());
    }

    @Test
    void testDesactivarBorraLosCodigosPendientes() {
        usuario.setDosPasos(true);
        Mockito.when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        Mockito.when(passwordEncoder.matches("1234", "hash-de-la-clave")).thenReturn(true);

        service.cambiarEstado(5L, false, "1234");

        assertThat(usuario.isDosPasos(), is(false));
        Mockito.verify(codigoRepository).deleteByUsuarioId(5L);
    }

    @Test
    void testConLaContrasenaIncorrectaNoSeCambiaNada() {
        Mockito.when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        Mockito.when(passwordEncoder.matches("mala", "hash-de-la-clave")).thenReturn(false);

        DosPasosException error = assertThrows(DosPasosException.class,
                () -> service.cambiarEstado(5L, true, "mala"));

        assertThat(error.getMessage(), is(equalTo("La contraseña no es correcta")));
        assertThat(usuario.isDosPasos(), is(false));
        Mockito.verify(usuarioRepository, Mockito.never()).save(ArgumentMatchers.any(UsuarioEntity.class));
    }

    @Test
    void testSiLaPersonaNoExisteLanzaError() {
        Mockito.when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DosPasosException.class, () -> service.cambiarEstado(99L, true, "1234"));
    }
}
