package dev.andrea.acompaname_backend.passwordreset;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.mail.CorreoException;
import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionPasswordInvalida;

@Service
public class RecuperacionPasswordServiceImpl implements RecuperacionPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordServiceImpl.class);

    private static final String ASUNTO = "Recupera tu contraseña de Acompáñame";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicioDeCorreo correo;
    private final long minutosDeValidez;
    private final String urlDelFront;

    public RecuperacionPasswordServiceImpl(UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository, PasswordEncoder passwordEncoder, ServicioDeCorreo correo,
            @Value("${password-reset.minutes}") long minutosDeValidez,
            @Value("${app.frontend-url}") String urlDelFront) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.minutosDeValidez = minutosDeValidez;
        this.urlDelFront = urlDelFront;
    }

    @Override
    @Transactional
    public void solicitarRestablecimiento(String email) {
        if (email == null) {
            return;
        }
        Optional<UsuarioEntity> encontrado = usuarioRepository.findByEmail(email.trim());

        // Si el correo no existe o la cuenta está bloqueada no se hace nada ni se avisa:
        // la respuesta es la misma que si existiera, así no se puede saber quién tiene cuenta
        if (encontrado.isEmpty() || !encontrado.get().isActivo()) {
            return;
        }
        UsuarioEntity usuario = encontrado.get();

        // Solo vale el último enlace pedido: los anteriores se borran
        tokenRepository.deleteByUsuarioId(usuario.getId());

        String token = CodificadorDeTokens.crear();
        PasswordResetTokenEntity entidad = PasswordResetTokenEntity.builder()
                .tokenHash(CodificadorDeTokens.hash(token))
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(minutosDeValidez))
                .build();
        tokenRepository.save(entidad);

        String enlace = urlDelFront + "/restablecer-password?token=" + token;
        try {
            correo.enviar(usuario.getEmail(), ASUNTO, construirTexto(usuario.getNombre(), enlace));
        } catch (CorreoException e) {
            // Si falla el envío no se cuenta a quien lo pidió (sería una pista); queda en el log, sin el correo
            log.error("No se pudo enviar el correo de recuperación al usuario {}", usuario.getId(), e);
        }
    }

    @Override
    @Transactional
    public void restablecer(String token, String passwordNueva) {
        if (token == null || token.isBlank()) {
            throw new TokenRecuperacionInvalidoException();
        }
        PasswordResetTokenEntity entidad = tokenRepository.findByTokenHash(CodificadorDeTokens.hash(token))
                .orElseThrow(TokenRecuperacionInvalidoException::new);
        UsuarioEntity usuario = entidad.getUsuario();

        if (entidad.estaCaducado(LocalDateTime.now()) || !usuario.isActivo()) {
            throw new TokenRecuperacionInvalidoException();
        }
        if (passwordEncoder.matches(passwordNueva, usuario.getPassword())) {
            throw new UsuarioExceptionPasswordInvalida("La nueva contraseña debe ser distinta de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        // Las sesiones abiertas dejan de valer: quien restablece la contraseña suele hacerlo porque no se fía
        usuario.cerrarSesiones();
        usuarioRepository.save(usuario);

        // El enlace solo se puede usar una vez: se borran todos los de esa persona
        tokenRepository.deleteByUsuarioId(usuario.getId());
    }

    private String construirTexto(String nombre, String enlace) {
        return "Hola " + nombre + ",\n\n"
                + "Hemos recibido una petición para restablecer tu contraseña. "
                + "Pulsa en este enlace para elegir una nueva (caduca en " + minutosDeValidez + " minutos):\n\n"
                + enlace + "\n\n"
                + "Si no has sido tú, ignora este correo: tu contraseña no cambiará.\n\n"
                + "El equipo de Acompáñame";
    }
}
