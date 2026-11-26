package dev.andrea.acompaname_backend.schedule.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// La dirección secreta del calendario (formato .ics) que da Google, Outlook o iCloud
public record VincularCalendarioDTORequest(
        @NotBlank(message = "Pega la dirección de tu calendario") @Size(max = 500, message = "La dirección del calendario es demasiado larga") String url) {
}
