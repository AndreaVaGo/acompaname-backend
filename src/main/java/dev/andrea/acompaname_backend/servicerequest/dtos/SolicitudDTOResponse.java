package dev.andrea.acompaname_backend.servicerequest.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;

public record SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                List<String> necesidades) {

        // Constructor sin necesidades, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, new ArrayList<>());
        }

        // Constructor sin fecha de fin ni necesidades, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, EstadoSolicitud estado, Long familiaId,
                        Long cuidadorId, String familiaNombre, String cuidadorNombre) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, null, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre);
        }
}