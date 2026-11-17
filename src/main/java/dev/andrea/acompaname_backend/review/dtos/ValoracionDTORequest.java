package dev.andrea.acompaname_backend.review.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValoracionDTORequest(
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres") String comentario,
        @NotNull(message = "La puntuacion de la valoracion no puede ser nulo") @Min(value = 1, message = "La puntuacion minima es 1") @Max(value = 5, message = "La puntuacion maxima es 5") Integer puntuacion,
        @NotNull(message = "El id de la solicitud no puede ser nulo") Long solicitudId

) {
}
