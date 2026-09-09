package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PerfilCuidadorDTORequest(

        @NotBlank(message = "La especialidad no puede estar vacío") @Size(max = 100, message = "La especialidad no puede superar 100 caracteres") String especialidad,

        @NotNull(message = "Los años de experiencia no puede ser nulo") Integer anosExperiencia,

        @NotNull(message = "La tarifa/hora no puede ser nulo") BigDecimal tarifaHora,

        @NotBlank(message = "La bio no puede estar vacío") @Size(max = 1000, message = "La biografía no puede superar 1000 caracteres") String bio,

        boolean tieneVehiculo,

        boolean disponibleAhora,

        @NotNull(message = "El usuario id no puede ser nulo") Long usuarioId

) {
}
