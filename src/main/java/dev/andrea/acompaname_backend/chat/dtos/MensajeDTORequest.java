package dev.andrea.acompaname_backend.chat.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensajeDTORequest(

        @NotBlank(message = "El mensaje no puede estar vacío") @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres") String texto

) {
}
