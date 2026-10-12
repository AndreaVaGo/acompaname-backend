package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PerfilCuidadorDTORequest(

        @NotBlank(message = "La especialidad no puede estar vacío") @Size(max = 100, message = "La especialidad no puede superar 100 caracteres") String especialidad,

        @NotNull(message = "Los años de experiencia no puede ser nulo") @PositiveOrZero(message = "Los años de experiencia no pueden ser negativos") Integer anosExperiencia,

        @NotNull(message = "La tarifa/hora no puede ser nulo") @PositiveOrZero(message = "La tarifa/hora no puede ser negativa") BigDecimal tarifaHora,

        @NotBlank(message = "La bio no puede estar vacío") @Size(max = 1000, message = "La biografía no puede superar 1000 caracteres") String bio,

        boolean tieneVehiculo,

        boolean disponibleAhora,

        @NotNull(message = "El usuario id no puede ser nulo") Long usuarioId,

        @Size(max = 500000, message = "La foto es demasiado grande") String foto,

        @Pattern(regexp = "hospitalario|domicilio|ambos", message = "El tipo de cuidado debe ser hospitalario, domicilio o ambos") String tipoCuidado

) {

    // Constructor sin tipo de cuidado: el mismo que había antes de añadir este campo
    public PerfilCuidadorDTORequest(String especialidad, Integer anosExperiencia, BigDecimal tarifaHora, String bio,
            boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String foto) {
        this(especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId, foto, null);
    }

    // Constructor sin foto: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTORequest(String especialidad, Integer anosExperiencia, BigDecimal tarifaHora, String bio,
            boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId) {
        this(especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId, null, null);
    }
}