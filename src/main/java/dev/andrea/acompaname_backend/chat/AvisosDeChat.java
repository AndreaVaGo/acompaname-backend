package dev.andrea.acompaname_backend.chat;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Manda un correo a la otra persona cuando le escriben en el chat de una solicitud.
// No incluye el texto del mensaje (es privado): solo avisa y enlaza al chat.
// Si el correo falla, solo se anota en el log: el mensaje nunca debe perderse por un correo.
@Component
public class AvisosDeChat {

    private static final Logger log = LoggerFactory.getLogger(AvisosDeChat.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FIRMA = "El equipo de Acompáñame";

    private final ServicioDeCorreo correo;
    private final String urlDelFront;

    public AvisosDeChat(ServicioDeCorreo correo, @Value("${app.frontend-url}") String urlDelFront) {
        this.correo = correo;
        this.urlDelFront = urlDelFront;
    }

    // A quien no ha escrito: "te han escrito en el chat"
    public void avisarMensajeNuevo(SolicitudEntity solicitud, UsuarioEntity emisor) {
        try {
            UsuarioEntity destinatario = destinatarioDe(solicitud, emisor);
            String texto = "Hola " + destinatario.getNombre() + ",\n\n"
                    + emisor.getNombre() + " te ha escrito en el chat de la solicitud de cuidado para el "
                    + solicitud.getFechaCuidado().format(FORMATO_FECHA) + ".\n\n"
                    + "Entra para leerlo y responder:\n"
                    + urlDelFront + "/chat/" + solicitud.getId() + "\n\n"
                    + "Si te escribe más veces, no te avisaremos de nuevo hasta que leas este mensaje.\n\n"
                    + FIRMA;
            correo.enviar(destinatario.getEmail(), "Tienes un mensaje nuevo en Acompáñame", texto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de mensaje nuevo (solicitud {})", solicitud.getId(), e);
        }
    }

    // Si escribe la familia, el aviso va a la persona cuidadora; y al revés
    private UsuarioEntity destinatarioDe(SolicitudEntity solicitud, UsuarioEntity emisor) {
        UsuarioEntity familia = solicitud.getFamilia();
        if (familia.getEmail().equals(emisor.getEmail())) {
            return solicitud.getCuidador().getUsuario();
        }
        return familia;
    }
}
