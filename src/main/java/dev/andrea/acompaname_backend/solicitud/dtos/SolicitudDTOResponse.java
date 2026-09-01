package dev.andrea.acompaname_backend.solicitud.dtos;

import java.time.LocalDate;

import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;

public record SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
        Integer edadPaciente, LocalDate fechaCuidado, EstadoSolicitud estado, Long familiaId, Long cuidadorId) {

}
