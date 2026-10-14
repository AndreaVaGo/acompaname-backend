package dev.andrea.acompaname_backend.solicitud.dtos;

import java.time.LocalDate;

import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;

public record SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre) {

        // Constructor sin fecha de fin, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, EstadoSolicitud estado, Long familiaId,
                        Long cuidadorId, String familiaNombre, String cuidadorNombre) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, null, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre);
        }
}