package dev.andrea.acompaname_backend.valoracion.dtos;

import java.time.LocalDate;

public record ValoracionDTOResponse(Long id, String comentario, Integer puntuacion, LocalDate fecha, Long solicitudId) {

}
