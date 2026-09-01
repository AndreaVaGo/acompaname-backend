package dev.andrea.acompaname_backend.solicitud.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudDTORequest(

        @NotBlank(message = "El tipo de cuidado no puede estar vacio") String tipoCuidado,

        @NotBlank(message = "El nombre del paciente no puede estar vacio") String nombrePaciente,

        String notas,

        @NotNull(message = "La edad del paciente no puede ser nulo") Integer edadPaciente,

        @NotNull(message = "La fecha del cuidado no puede ser nulo") LocalDate fechaCuidado,

        @NotNull(message = "El id de la familia no puede ser nulo") Long familiaId,

        @NotNull(message = "El id del cuidado no puede ser nulo") Long cuidadorId

) {


}