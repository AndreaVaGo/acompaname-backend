package dev.andrea.acompaname_backend.schedule.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

public record BloqueOcupadoDTOResponse(
        Long id,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo) {
}
