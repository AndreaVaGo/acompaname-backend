package dev.andrea.acompaname_backend.user;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.auth.CodigoLoginRepository;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;
import dev.andrea.acompaname_backend.schedule.BloqueOcupadoRepository;
import dev.andrea.acompaname_backend.suggestion.SugerenciaRepository;
import dev.andrea.acompaname_backend.verificacion.VerificacionRepository;

// Borrar la cuenta de una persona NO borra su fila: las solicitudes, los pagos, los fichajes, los mensajes
// y las valoraciones de la otra parte apuntan a ella, y esa persona no tiene que perder su historial.
// Lo que se hace es anonimizarla: se borran sus datos personales y la cuenta queda inactiva.
@Component
public class AnonimizadorDeCuenta {

    static final String NOMBRE_ELIMINADA = "Cuenta eliminada";
    // .invalid es un dominio reservado que nunca existe: ningún correo llega ahí
    static final String DOMINIO_ELIMINADA = "@eliminado.acompaname.invalid";

    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;
    private final CodigoLoginRepository codigoLoginRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final SugerenciaRepository sugerenciaRepository;
    private final VerificacionRepository verificacionRepository;
    private final BloqueOcupadoRepository bloqueOcupadoRepository;
    private final PasswordEncoder passwordEncoder;

    public AnonimizadorDeCuenta(UsuarioRepository usuarioRepository,
            PerfilCuidadorRepository perfilCuidadorRepository, CodigoLoginRepository codigoLoginRepository,
            PasswordResetTokenRepository tokenRepository, SugerenciaRepository sugerenciaRepository,
            VerificacionRepository verificacionRepository, BloqueOcupadoRepository bloqueOcupadoRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
        this.codigoLoginRepository = codigoLoginRepository;
        this.tokenRepository = tokenRepository;
        this.sugerenciaRepository = sugerenciaRepository;
        this.verificacionRepository = verificacionRepository;
        this.bloqueOcupadoRepository = bloqueOcupadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void anonimizar(UsuarioEntity usuario) {
        // Lo que solo servía a esta persona se borra del todo
        codigoLoginRepository.deleteByUsuarioId(usuario.getId());
        tokenRepository.deleteByUsuarioId(usuario.getId());
        sugerenciaRepository.deleteByUsuarioId(usuario.getId());

        // El perfil se busca por el email, así que hay que hacerlo antes de cambiarlo
        perfilCuidadorRepository.findByUsuarioEmail(usuario.getEmail()).ifPresent(this::vaciarPerfil);

        usuario.setNombre(NOMBRE_ELIMINADA);
        usuario.setEmail("eliminado-" + usuario.getId() + DOMINIO_ELIMINADA);
        usuario.setTelefono(null);
        // Una contraseña que nadie conoce: aunque alguien reactivara la cuenta, no se podría entrar
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setDosPasos(false);
        // Inactiva: sus tokens dejan de valer al momento y no puede iniciar sesión
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private void vaciarPerfil(PerfilCuidadorEntity perfil) {
        verificacionRepository.deleteByCuidadorId(perfil.getId());
        bloqueOcupadoRepository.deleteByCuidadorId(perfil.getId());

        perfil.setFoto(null);
        perfil.setBio("");
        perfil.setCiudad(null);
        perfil.setSexo(null);
        perfil.setTitulaciones(null);
        perfil.setOtrasTitulaciones(null);
        perfil.setDisponibleAhora(false);
        perfilCuidadorRepository.save(perfil);
    }
}
