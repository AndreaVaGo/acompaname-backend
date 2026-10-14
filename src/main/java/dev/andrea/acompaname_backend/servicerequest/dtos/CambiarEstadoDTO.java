package dev.andrea.acompaname_backend.servicerequest.dtos;

import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoDTO(@NotNull(message = "El estado no puede ser nulo") EstadoSolicitud estado) {

}
