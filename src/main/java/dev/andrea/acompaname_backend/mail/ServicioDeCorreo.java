package dev.andrea.acompaname_backend.mail;

// Envía correos de texto. Tiene dos versiones:
// - ServicioDeCorreoEnLog (por defecto, para desarrollo): no envía nada, escribe el correo en el log
// - ServicioDeCorreoSmtp (cuando mail.enabled=true, para producción): lo envía de verdad por SMTP
public interface ServicioDeCorreo {

    void enviar(String destino, String asunto, String texto);
}
