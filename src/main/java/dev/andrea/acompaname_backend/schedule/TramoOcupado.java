package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

// Un rato ocupado leído de un calendario externo: un solo día, con su hora de inicio y de fin.
// Solo lleva fecha y horas: el título y los detalles de la cita no se leen nunca
public record TramoOcupado(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
}
