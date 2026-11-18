package dev.andrea.acompaname_backend.servicerequest;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Manda los correos de aviso cuando cambia una solicitud:
// - solicitud nueva: avisa a la persona cuidadora
// - solicitud aceptada o rechazada: avisa a la familia
// Si el correo falla, solo se anota en el log: la solicitud nunca debe romperse por un correo.
@Component
public class AvisosDeSolicitud {

    private static final Logger log = LoggerFactory.getLogger(AvisosDeSolicitud.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FIRMA = "El equipo de Acompáñame";

    private final ServicioDeCorreo correo;
    private final String urlDelFront;

    public AvisosDeSolicitud(ServicioDeCorreo correo, @Value("${app.frontend-url}") String urlDelFront) {
        this.correo = correo;
        this.urlDelFront = urlDelFront;
    }

    // Al cuidador: ha llegado una solicitud nueva
    public void avisarSolicitudNueva(SolicitudEntity solicitud) {
        try {
            UsuarioEntity cuidador = solicitud.getCuidador().getUsuario();
            String texto = "Hola " + cuidador.getNombre() + ",\n\n"
                    + solicitud.getFamilia().getNombre() + " te ha enviado una solicitud de cuidado"
                    + tipoDe(solicitud) + personasDe(solicitud) + " " + cuandoDe(solicitud) + ".\n\n"
                    + "Entra para verla y decidir si la aceptas:\n"
                    + urlDelFront + "/solicitudes-cuidador\n\n"
                    + FIRMA;
            correo.enviar(cuidador.getEmail(), "Tienes una solicitud nueva en Acompáñame", texto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de solicitud nueva (solicitud {})", solicitud.getId(), e);
        }
    }

    // A la familia: la persona cuidadora ha aceptado su solicitud
    public void avisarSolicitudAceptada(SolicitudEntity solicitud) {
        try {
            UsuarioEntity familia = solicitud.getFamilia();
            String texto = "Hola " + familia.getNombre() + ",\n\n"
                    + "¡Buenas noticias! " + solicitud.getCuidador().getUsuario().getNombre()
                    + " ha aceptado tu solicitud de cuidado" + tipoDe(solicitud) + personasDe(solicitud) + " " + cuandoDe(solicitud) + ".\n\n"
                    + "Puedes ver los detalles aquí:\n"
                    + urlDelFront + "/solicitudes\n\n"
                    + FIRMA;
            correo.enviar(familia.getEmail(), "Tu solicitud ha sido aceptada", texto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de solicitud aceptada (solicitud {})", solicitud.getId(), e);
        }
    }

    // A la familia: la persona cuidadora no ha podido aceptar su solicitud
    public void avisarSolicitudRechazada(SolicitudEntity solicitud) {
        try {
            UsuarioEntity familia = solicitud.getFamilia();
            String texto = "Hola " + familia.getNombre() + ",\n\n"
                    + solicitud.getCuidador().getUsuario().getNombre()
                    + " no puede atender tu solicitud de cuidado" + tipoDe(solicitud) + personasDe(solicitud) + " " + cuandoDe(solicitud)
                    + ".\n\n"
                    + "No te preocupes: puedes buscar a otra persona cuidadora aquí:\n"
                    + urlDelFront + "/buscar\n\n"
                    + FIRMA;
            correo.enviar(familia.getEmail(), "Tu solicitud no ha podido ser aceptada", texto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de solicitud rechazada (solicitud {})", solicitud.getId(), e);
        }
    }

    // " (Acompañamiento)", o nada si la solicitud no tiene tipo de cuidado
    private String tipoDe(SolicitudEntity solicitud) {
        String tipo = solicitud.getTipoCuidado();
        if (tipo == null || tipo.isBlank()) {
            return "";
        }
        return " (" + tipo + ")";
    }

    // " de 3 personas" si se cuida a varias personas, o nada si es una sola
    private String personasDe(SolicitudEntity solicitud) {
        int numero = PersonasDeSolicitud.aDTO(solicitud).size();
        if (numero <= 1) {
            return "";
        }
        return " de " + numero + " personas";
    }

    // "para el 15/12/2026" si es un solo día, o "del 15/12/2026 al 18/12/2026" si son varios
    private String cuandoDe(SolicitudEntity solicitud) {
        String inicio = solicitud.getFechaCuidado().format(FORMATO_FECHA);
        if (solicitud.getFechaFin() == null || solicitud.getFechaFin().equals(solicitud.getFechaCuidado())) {
            return "para el " + inicio;
        }
        return "del " + inicio + " al " + solicitud.getFechaFin().format(FORMATO_FECHA);
    }
}
