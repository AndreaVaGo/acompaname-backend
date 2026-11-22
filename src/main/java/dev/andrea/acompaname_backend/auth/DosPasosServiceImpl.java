package dev.andrea.acompaname_backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.passwordreset.CodificadorDeTokens;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@Service
public class DosPasosServiceImpl implements DosPasosService {

    private static final String ASUNTO = "Tu código para entrar en Acompáñame";

    // SecureRandom es el generador de números al azar preparado para seguridad (Random no vale)
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CodigoLoginRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicioDeCorreo correo;
    private final long minutosDeValidez;
    private final int maximoDeIntentos;

    public DosPasosServiceImpl(CodigoLoginRepository codigoRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, ServicioDeCorreo correo,
            @Value("${dos-pasos.minutes}") long minutosDeValidez,
            @Value("${dos-pasos.max-attempts}") int maximoDeIntentos) {
        this.codigoRepository = codigoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.minutosDeValidez = minutosDeValidez;
        this.maximoDeIntentos = maximoDeIntentos;
    }

    @Override
    @Transactional
    public String iniciar(UsuarioEntity usuario) {
        // Solo vale el último código pedido: los anteriores se borran
        codigoRepository.deleteByUsuarioId(usuario.getId());

        String desafio = CodificadorDeTokens.crear();
        String codigo = crearCodigo();
        CodigoLoginEntity entidad = CodigoLoginEntity.builder()
                .desafioHash(CodificadorDeTokens.hash(desafio))
                .codigoHash(resumenDelCodigo(desafio, codigo))
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(minutosDeValidez))
                .build();
        codigoRepository.save(entidad);

        // Si el correo no se puede enviar, el error sube y el login falla: sin el código la persona no podría entrar
        correo.enviar(usuario.getEmail(), ASUNTO, construirTexto(usuario.getNombre(), codigo));
        return desafio;
    }

    // noRollbackFor: cuando el código es incorrecto se lanza una excepción, pero hay que guardar
    // igualmente el intento fallido. Sin esto, la base de datos deshacería el cambio y nunca se gastarían los intentos
    @Override
    @Transactional(noRollbackFor = CodigoInvalidoException.class)
    public UsuarioEntity verificar(String desafio, String codigo) {
        if (desafio == null || desafio.isBlank() || codigo == null || codigo.isBlank()) {
            throw new CodigoInvalidoException();
        }
        CodigoLoginEntity entidad = codigoRepository.findByDesafioHash(CodificadorDeTokens.hash(desafio))
                .orElseThrow(CodigoInvalidoException::new);
        UsuarioEntity usuario = entidad.getUsuario();

        if (entidad.estaCaducado(LocalDateTime.now()) || !usuario.isActivo()) {
            codigoRepository.delete(entidad);
            throw new CodigoInvalidoException();
        }

        if (!sonIguales(entidad.getCodigoHash(), resumenDelCodigo(desafio, codigo.trim()))) {
            entidad.sumarIntento();
            if (entidad.getIntentos() >= maximoDeIntentos) {
                // Demasiados fallos: el código se quema y hay que volver a iniciar sesión
                codigoRepository.delete(entidad);
            } else {
                codigoRepository.save(entidad);
            }
            throw new CodigoInvalidoException();
        }

        // El código solo se puede usar una vez
        codigoRepository.delete(entidad);
        return usuario;
    }

    @Override
    @Transactional
    public void cambiarEstado(Long usuarioId, boolean activar, String password) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new DosPasosException("No se ha encontrado tu cuenta"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new DosPasosException("La contraseña no es correcta");
        }

        usuario.setDosPasos(activar);
        usuarioRepository.save(usuario);

        if (!activar) {
            // Si lo desactiva, no queda ningún código pendiente
            codigoRepository.deleteByUsuarioId(usuarioId);
        }
    }

    // Código de 6 cifras al azar, con ceros por delante si hace falta (por ejemplo 004821)
    private String crearCodigo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }

    // El resumen del código se calcula junto con el desafío: dos personas con el mismo código dan resúmenes distintos
    private String resumenDelCodigo(String desafio, String codigo) {
        return CodificadorDeTokens.hash(desafio + ":" + codigo);
    }

    // Compara los dos resúmenes sin parar en la primera letra distinta, para que el tiempo que tarda no dé pistas
    private boolean sonIguales(String esperado, String recibido) {
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    private String construirTexto(String nombre, String codigo) {
        return "Hola " + nombre + ",\n\n"
                + "Tu código para entrar en Acompáñame es: " + codigo + "\n\n"
                + "Caduca en " + minutosDeValidez + " minutos y solo se puede usar una vez.\n\n"
                + "Si no has sido tú quien ha intentado entrar, cambia tu contraseña.\n\n"
                + "El equipo de Acompáñame";
    }
}
