package dev.andrea.acompaname_backend.limpieza;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.auth.CodigoLoginRepository;
import dev.andrea.acompaname_backend.passwordreset.PasswordResetTokenRepository;

// Los enlaces de "Olvidé mi contraseña" y los códigos del inicio de sesión en dos pasos caducan,
// pero nada los borraba: la base de datos los iba acumulando para siempre.
// Esta tarea los borra una vez al día. Una caducada ya no sirve para nada, así que no se pierde nada.
@Component
public class LimpiezaDeCaducados {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDeCaducados.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final CodigoLoginRepository codigoLoginRepository;

    public LimpiezaDeCaducados(PasswordResetTokenRepository tokenRepository,
            CodigoLoginRepository codigoLoginRepository) {
        this.tokenRepository = tokenRepository;
        this.codigoLoginRepository = codigoLoginRepository;
    }

    // Por defecto, cada noche a las 03:30. Se puede cambiar con la variable LIMPIEZA_CRON
    @Scheduled(cron = "${limpieza.cron}")
    public void limpiarCadaNoche() {
        limpiar(LocalDateTime.now());
    }

    // Cada borrado abre su propia transacción (está en el repositorio), así que esto funciona
    // tanto llamado por la tarea programada como desde un test
    void limpiar(LocalDateTime ahora) {
        int tokens = tokenRepository.borrarCaducados(ahora);
        int codigos = codigoLoginRepository.borrarCaducados(ahora);
        log.info("Limpieza de caducados: {} enlaces de recuperación y {} códigos de acceso borrados", tokens, codigos);
    }
}
