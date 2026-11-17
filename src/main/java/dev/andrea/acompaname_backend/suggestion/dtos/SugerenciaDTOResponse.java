package dev.andrea.acompaname_backend.suggestion.dtos;

import java.time.LocalDateTime;

// Lo que ve la persona que envía la sugerencia
public record SugerenciaDTOResponse(
        Long id,
        String texto,
        LocalDateTime fecha) {
}
