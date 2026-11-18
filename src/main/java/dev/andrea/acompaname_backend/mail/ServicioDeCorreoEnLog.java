package dev.andrea.acompaname_backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Versión para desarrollo y tests: no envía nada, solo escribe el correo en el log.
// Así se puede probar "Olvidé mi contraseña" sin tener un servidor de correo: el enlace sale en la consola.
// Está activa mientras mail.enabled no sea "true".
@Service
@ConditionalOnProperty(name = "mail.enabled", havingValue = "false", matchIfMissing = true)
public class ServicioDeCorreoEnLog implements ServicioDeCorreo {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeCorreoEnLog.class);

    @Override
    public void enviar(String destino, String asunto, String texto) {
        log.info("[CORREO SIMULADO] Para: {} | Asunto: {}\n{}", destino, asunto, texto);
    }
}
