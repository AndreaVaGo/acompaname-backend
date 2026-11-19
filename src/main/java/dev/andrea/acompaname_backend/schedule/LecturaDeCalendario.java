package dev.andrea.acompaname_backend.schedule;

import java.util.List;

// Lo que sale de leer un archivo .ics: los ratos ocupados y cuántos eventos se han dejado fuera
// (repetidos, cancelados, marcados como libres o que no se han podido entender)
public record LecturaDeCalendario(List<TramoOcupado> tramos, int omitidos) {
}
