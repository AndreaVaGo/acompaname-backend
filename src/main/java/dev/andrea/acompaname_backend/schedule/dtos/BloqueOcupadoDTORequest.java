package dev.andrea.acompaname_backend.schedule.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BloqueOcupadoDTORequest(
        @NotNull(message = "La fecha no puede ser nula") LocalDate fecha,
        @NotNull(message = "La hora de inicio no puede ser nula") LocalTime horaInicio,
        @NotNull(message = "La hora de fin no puede ser nula") LocalTime horaFin,
        @Size(max = 100, message = "El motivo no puede superar 100 caracteres") String motivo) {
}
