package dev.andrea.acompaname_backend.schedule;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

// Lee un archivo de calendario (.ics) y saca de él los ratos ocupados.
// Solo se queda con la fecha y las horas: el título, el lugar y las notas de las citas no se leen.
// Reglas:
// - Las horas en UTC (acaban en Z) o en otra zona horaria se pasan a la hora de la aplicación (Madrid).
// - Un evento de varios días se parte en un bloque por día.
// - Un evento de todo el día ocupa de 00:00 a 23:59.
// - Los eventos repetidos, cancelados, marcados como "libre" o que no se entienden se omiten y se cuentan.
public class LectorDeCalendario {

    private static final ZoneId ZONA_DE_LA_APLICACION = ZoneId.of("Europe/Madrid");
    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59);
    private static final DateTimeFormatter FECHA_Y_HORA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    // Un evento que dura más de esto se considera un error del archivo y se omite
    static final int MAXIMO_DIAS_POR_EVENTO = 31;
    // Máximo de bloques que se aceptan de una sola vez
    static final int MAXIMO_TRAMOS = 500;

    // Los datos de un evento mientras se va leyendo
    private static class Evento {
        String propiedadInicio;
        String valorInicio;
        String propiedadFin;
        String valorFin;
        boolean repetido;
        boolean cancelado;
        boolean libre;
    }

    public LecturaDeCalendario leer(String contenido) {
        if (contenido == null || !contenido.contains("BEGIN:VCALENDAR")) {
            throw new BloqueOcupadoExceptionFechaInvalida("El archivo no parece un calendario .ics válido");
        }

        List<TramoOcupado> tramos = new ArrayList<>();
        int omitidos = 0;
        Evento evento = null;

        for (String linea : juntarLineas(contenido)) {
            String texto = linea.trim();
            if (texto.equals("BEGIN:VEVENT")) {
                evento = new Evento();
            } else if (texto.equals("END:VEVENT")) {
                if (evento != null) {
                    int antes = tramos.size();
                    boolean valido = convertir(evento, tramos);
                    if (!valido || tramos.size() == antes) {
                        omitidos++;
                    }
                }
                evento = null;
                if (tramos.size() > MAXIMO_TRAMOS) {
                    throw new BloqueOcupadoExceptionFechaInvalida(
                            "El calendario tiene demasiados eventos. Exporta solo los próximos meses");
                }
            } else if (evento != null) {
                leerPropiedad(texto, evento);
            }
        }
        return new LecturaDeCalendario(tramos, omitidos);
    }

    // En un .ics una línea larga se parte en varias, y las que siguen empiezan por un espacio o un tabulador.
    // Aquí se vuelven a pegar para tener una línea por propiedad
    private List<String> juntarLineas(String contenido) {
        String normalizado = contenido.replace("\r\n", "\n").replace("\r", "\n");
        List<String> lineas = new ArrayList<>();
        for (String linea : normalizado.split("\n")) {
            boolean continuacion = !lineas.isEmpty() && (linea.startsWith(" ") || linea.startsWith("\t"));
            if (continuacion) {
                int ultima = lineas.size() - 1;
                lineas.set(ultima, lineas.get(ultima) + linea.substring(1));
            } else {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    // Guarda en el evento solo las propiedades que hacen falta; el resto (título, lugar...) se ignora
    private void leerPropiedad(String linea, Evento evento) {
        int dosPuntos = linea.indexOf(':');
        if (dosPuntos < 0) {
            return;
        }
        String propiedad = linea.substring(0, dosPuntos);
        String valor = linea.substring(dosPuntos + 1).trim();
        String nombre = propiedad;
        int puntoYComa = propiedad.indexOf(';');
        if (puntoYComa >= 0) {
            nombre = propiedad.substring(0, puntoYComa);
        }
        nombre = nombre.toUpperCase();

        if (nombre.equals("DTSTART")) {
            evento.propiedadInicio = propiedad;
            evento.valorInicio = valor;
        } else if (nombre.equals("DTEND")) {
            evento.propiedadFin = propiedad;
            evento.valorFin = valor;
        } else if (nombre.equals("RRULE")) {
            evento.repetido = true;
        } else if (nombre.equals("STATUS") && valor.equalsIgnoreCase("CANCELLED")) {
            evento.cancelado = true;
        } else if (nombre.equals("TRANSP") && valor.equalsIgnoreCase("TRANSPARENT")) {
            evento.libre = true;
        }
    }

    // Convierte un evento en uno o varios bloques y los añade a la lista.
    // Devuelve false si el evento se omite (no se ha podido entender o no cuenta)
    private boolean convertir(Evento evento, List<TramoOcupado> tramos) {
        if (evento.repetido || evento.cancelado || evento.libre || evento.valorInicio == null) {
            return false;
        }

        boolean todoElDia = evento.valorInicio.length() == 8;
        LocalDateTime inicio = leerMomento(evento.propiedadInicio, evento.valorInicio);
        if (inicio == null) {
            return false;
        }

        LocalDateTime fin;
        if (evento.valorFin != null) {
            fin = leerMomento(evento.propiedadFin, evento.valorFin);
        } else if (todoElDia) {
            // Un evento de todo el día sin fin dura ese día
            fin = inicio.plusDays(1);
        } else {
            fin = null;
        }
        if (fin == null || !fin.isAfter(inicio)) {
            return false;
        }

        // Si acaba justo a las 00:00, ese último día no está ocupado
        LocalDate primerDia = inicio.toLocalDate();
        LocalDate ultimoDia = fin.toLocalDate();
        if (fin.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            ultimoDia = ultimoDia.minusDays(1);
        }
        if (primerDia.plusDays(MAXIMO_DIAS_POR_EVENTO).isBefore(ultimoDia)) {
            return false;
        }

        LocalDate dia = primerDia;
        while (!dia.isAfter(ultimoDia)) {
            LocalTime desde = LocalTime.MIDNIGHT;
            if (dia.equals(primerDia)) {
                desde = inicio.toLocalTime();
            }
            LocalTime hasta = FIN_DEL_DIA;
            if (dia.equals(fin.toLocalDate())) {
                hasta = fin.toLocalTime();
            }
            // Un rato de cero minutos no ocupa nada
            if (hasta.isAfter(desde)) {
                tramos.add(new TramoOcupado(dia, desde, hasta));
            }
            dia = dia.plusDays(1);
        }
        return true;
    }

    // Convierte un valor como 20261201T090000Z, 20261201T090000 o 20261201 (con su zona horaria, si la trae)
    // en una fecha y hora de la zona de la aplicación. Devuelve null si no se entiende
    private LocalDateTime leerMomento(String propiedad, String valor) {
        try {
            if (valor.length() == 8) {
                return LocalDate.parse(valor, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
            }
            if (valor.endsWith("Z")) {
                LocalDateTime enUtc = LocalDateTime.parse(valor.substring(0, valor.length() - 1), FECHA_Y_HORA);
                return enUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA_DE_LA_APLICACION).toLocalDateTime();
            }
            LocalDateTime local = LocalDateTime.parse(valor, FECHA_Y_HORA);
            String zona = sacarZona(propiedad);
            if (zona == null) {
                // Sin zona horaria se toma como hora de la aplicación
                return local;
            }
            try {
                return local.atZone(ZoneId.of(zona)).withZoneSameInstant(ZONA_DE_LA_APLICACION).toLocalDateTime();
            } catch (DateTimeException zonaDesconocida) {
                // Una zona que Java no conoce (por ejemplo "Romance Standard Time"): se toma la hora tal cual
                return local;
            }
        } catch (DateTimeParseException noSeEntiende) {
            return null;
        }
    }

    // De "DTSTART;TZID=Europe/Madrid" saca "Europe/Madrid". Si no hay zona, devuelve null
    private String sacarZona(String propiedad) {
        if (propiedad == null) {
            return null;
        }
        for (String parte : propiedad.split(";")) {
            if (parte.toUpperCase().startsWith("TZID=")) {
                return parte.substring(5).replace("\"", "");
            }
        }
        return null;
    }
}
