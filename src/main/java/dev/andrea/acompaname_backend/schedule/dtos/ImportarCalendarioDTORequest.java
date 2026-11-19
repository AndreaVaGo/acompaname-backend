package dev.andrea.acompaname_backend.schedule.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// El texto de un archivo .ics, tal cual lo exporta Google Calendar, Outlook o el calendario del móvil
public record ImportarCalendarioDTORequest(
        @NotBlank(message = "El calendario no puede estar vacío") @Size(max = 500000, message = "El archivo es demasiado grande") String contenido) {
}
