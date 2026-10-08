package dev.andrea.acompaname_backend.solicitud.dtos;

import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoDTO(@NotNull(message = "El estado no puede ser nulo") EstadoSolicitud estado) {

}
