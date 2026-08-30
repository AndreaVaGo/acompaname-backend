package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PerfilCuidadorDTORequest(

        @NotBlank(message = "La especialidad no puede estar vacío") String especialidad,

        @NotNull(message = "Los años de experiencia no puede ser nulo") Integer anosExperiencia,

        @NotNull(message = "La tarifa/hora no puede ser nulo") BigDecimal tarifaHora,

        @NotBlank(message = "La bio no puede estar vacío") String bio,

        boolean tieneVehiculo,

        boolean disponibleAhora,

        @NotNull(message = "El usuario id no puede ser nulo") Long usuarioId

) {
}
