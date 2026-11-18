package dev.andrea.acompaname_backend.payment;

import java.math.BigDecimal;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Manda el correo de aviso cuando la familia paga: avisa a la persona cuidadora de que ha recibido el pago.
// Si el correo falla, solo se anota en el log: el pago nunca debe romperse por un correo.
@Component
public class AvisosDePago {

    private static final Logger log = LoggerFactory.getLogger(AvisosDePago.class);
    private static final String FIRMA = "El equipo de Acompáñame";

    private final ServicioDeCorreo correo;
    private final String urlDelFront;

    public AvisosDePago(ServicioDeCorreo correo, @Value("${app.frontend-url}") String urlDelFront) {
        this.correo = correo;
        this.urlDelFront = urlDelFront;
    }

    // A la persona cuidadora: la familia ha pagado el servicio
    public void avisarPagoRecibido(PagoEntity pago) {
        try {
            UsuarioEntity cuidador = pago.getSolicitud().getCuidador().getUsuario();
            String texto = "Hola " + cuidador.getNombre() + ",\n\n"
                    + pago.getSolicitud().getFamilia().getNombre() + " ha pagado el servicio de cuidado por un "
                    + "importe de " + importeEnTexto(pago.getImporte()) + ".\n\n"
                    + "Puedes ver tus servicios en tu historial:\n"
                    + urlDelFront + "/historial\n\n"
                    + FIRMA;
            correo.enviar(cuidador.getEmail(), "Has recibido un pago en Acompáñame", texto);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de pago recibido (pago {})", pago.getId(), e);
        }
    }

    // "58,00 €" (con coma, como se escribe en España)
    private String importeEnTexto(BigDecimal importe) {
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f €", importe);
    }
}
