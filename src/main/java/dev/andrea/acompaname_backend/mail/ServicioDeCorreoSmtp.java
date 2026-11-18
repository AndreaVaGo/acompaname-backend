package dev.andrea.acompaname_backend.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Versión para producción: envía el correo de verdad por SMTP.
// Se activa con mail.enabled=true y usa los datos del servidor de spring.mail.* (variables MAIL_HOST, etc.)
@Service
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
public class ServicioDeCorreoSmtp implements ServicioDeCorreo {

    private final JavaMailSender enviador;
    private final String remitente;

    public ServicioDeCorreoSmtp(JavaMailSender enviador, @Value("${mail.from}") String remitente) {
        this.enviador = enviador;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destino, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        try {
            enviador.send(mensaje);
        } catch (MailException e) {
            throw new CorreoException("No se pudo enviar el correo", e);
        }
    }
}
