package dev.andrea.acompaname_backend.suggestion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SugerenciaDTORequest(
        @NotBlank(message = "La sugerencia no puede estar vacía") @Size(max = 1000, message = "La sugerencia no puede superar 1000 caracteres") String texto) {
}
