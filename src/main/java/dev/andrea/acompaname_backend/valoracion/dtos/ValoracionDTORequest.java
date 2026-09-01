package dev.andrea.acompaname_backend.valoracion.dtos;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record ValoracionDTORequest(
        String comentario,
        @NotNull(message = "La puntuacion de la valoracion no puede ser nulo") Integer puntuacion,
        @NotNull(message = "La fecha de la valoracion no puede ser nulo") LocalDate fecha,
        @NotNull(message = "El id de la solicitud no puede ser nulo") Long solicitudId

) {
}
