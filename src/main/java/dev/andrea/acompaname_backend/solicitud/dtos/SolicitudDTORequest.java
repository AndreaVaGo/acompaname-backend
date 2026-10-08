package dev.andrea.acompaname_backend.solicitud.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudDTORequest(

                @NotBlank(message = "El tipo de cuidado no puede estar vacio") String tipoCuidado,

                @NotBlank(message = "El nombre del paciente no puede estar vacio") @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String nombrePaciente,

                @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres") String notas,

                @NotNull(message = "La edad del paciente no puede ser nulo") @Min(value = 0, message = "La edad no puede ser negativa") @Max(value = 120, message = "La edad no puede superar 120 años") Integer edadPaciente,

                @NotNull(message = "La fecha del cuidado no puede ser nulo") @FutureOrPresent(message = "La fecha del cuidado no puede estar en el pasado") LocalDate fechaCuidado,

                @NotNull(message = "El id de la familia no puede ser nulo") Long familiaId,

                @NotNull(message = "El id del cuidado no puede ser nulo") Long cuidadorId

) 
{

}