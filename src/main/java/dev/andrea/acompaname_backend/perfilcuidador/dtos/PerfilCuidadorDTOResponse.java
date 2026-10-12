package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

public record PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
        String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
        String foto, String ciudad) {

    // Constructor sin foto ni ciudad: el mismo que había al principio
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, null, null);
    }

    // Constructor sin ciudad: el mismo que había antes de añadir este campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, null);
    }
}