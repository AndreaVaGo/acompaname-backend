package dev.andrea.acompaname_backend.geolocation;

// Calcula los kilómetros en línea recta entre dos puntos del mapa (fórmula de Haversine).
// Tiene en cuenta que la Tierra es redonda, por eso no vale restar las coordenadas sin más
public class Distancia {

    private static final double RADIO_DE_LA_TIERRA_KM = 6371.0;

    public static double enKilometros(Coordenadas a, Coordenadas b) {
        double diferenciaLatitud = Math.toRadians(b.latitud() - a.latitud());
        double diferenciaLongitud = Math.toRadians(b.longitud() - a.longitud());

        double parte = Math.sin(diferenciaLatitud / 2) * Math.sin(diferenciaLatitud / 2)
                + Math.cos(Math.toRadians(a.latitud())) * Math.cos(Math.toRadians(b.latitud()))
                        * Math.sin(diferenciaLongitud / 2) * Math.sin(diferenciaLongitud / 2);

        double angulo = 2 * Math.atan2(Math.sqrt(parte), Math.sqrt(1 - parte));
        return RADIO_DE_LA_TIERRA_KM * angulo;
    }
}
