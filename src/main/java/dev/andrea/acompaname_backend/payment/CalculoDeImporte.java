package dev.andrea.acompaname_backend.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

// Calcula el importe del pago de una solicitud:
//   horas del cuidado x tarifa por hora de la persona cuidadora
// La primera persona se cobra a tarifa completa y cada persona extra suma un recargo
// (por ejemplo 0,25 = un 25 % de la tarifa por cada persona de más).
@Component
public class CalculoDeImporte {

    // Recargo por defecto si no se configura nada: 25 % por cada persona extra
    public static final BigDecimal RECARGO_POR_DEFECTO = new BigDecimal("0.25");

    private final BigDecimal recargoPersonaExtra;

    public CalculoDeImporte(@Value("${pagos.recargo-persona-extra:0.25}") BigDecimal recargoPersonaExtra) {
        this.recargoPersonaExtra = recargoPersonaExtra;
    }

    public BigDecimal calcular(SolicitudEntity solicitud, BigDecimal tarifaHora) {
        long horas = horasDe(solicitud);
        int personasExtra = solicitud.numeroDePersonas() - 1;

        // factor = 1 + recargo x personas extra (con una persona, el factor es 1)
        BigDecimal factor = BigDecimal.ONE.add(recargoPersonaExtra.multiply(BigDecimal.valueOf(personasExtra)));

        return tarifaHora
                .multiply(BigDecimal.valueOf(horas))
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Horas que dura el cuidado, de inicio a fin. Una fracción cuenta como hora entera
    // y el mínimo es una hora
    public long horasDe(SolicitudEntity solicitud) {
        long minutos = Duration.between(solicitud.momentoInicio(), solicitud.momentoFin()).toMinutes();
        long horas = minutos / 60;
        if (minutos % 60 > 0) {
            horas = horas + 1;
        }
        if (horas < 1) {
            horas = 1;
        }
        return horas;
    }
}
