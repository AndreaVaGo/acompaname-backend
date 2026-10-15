package dev.andrea.acompaname_backend.review.dtos;

import java.time.LocalDate;

// Lo que cualquier visitante puede ver de una valoración: sin ids ni datos de la familia
public record ValoracionPublicaDTOResponse(Integer puntuacion, String comentario, LocalDate fecha) {

}