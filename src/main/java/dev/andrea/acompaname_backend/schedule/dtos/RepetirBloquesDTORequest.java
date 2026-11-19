package dev.andrea.acompaname_backend.schedule.dtos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Un mismo rato ocupado que se repite ciertos días de la semana entre dos fechas.
// Por ejemplo: "turno de mañanas" de lunes a viernes, del 1 al 31 de diciembre, de 07:00 a 15:00.
// Si la hora de fin es anterior a la de inicio (22:00 a 07:00) es un turno de noche que acaba al día siguiente.
public record RepetirBloquesDTORequest(
        @NotNull(message = "La fecha de inicio no puede ser nula") LocalDate desde,
        @NotNull(message = "La fecha de fin no puede ser nula") LocalDate hasta,
        @NotEmpty(message = "Elige al menos un día de la semana") Set<DayOfWeek> diasSemana,
        @NotNull(message = "La hora de inicio no puede ser nula") LocalTime horaInicio,
        @NotNull(message = "La hora de fin no puede ser nula") LocalTime horaFin,
        @Size(max = 100, message = "El motivo no puede superar 100 caracteres") String motivo) {
}
