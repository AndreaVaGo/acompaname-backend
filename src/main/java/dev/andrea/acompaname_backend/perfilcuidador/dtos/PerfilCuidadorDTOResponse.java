package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

public record PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
        String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
        String foto, Double valoracionMedia, Long numValoraciones) {

    // Constructor sin valoraciones: el mismo que había antes de añadir estos campos
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, null, 0L);
    }

    // Constructor sin foto: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, null, null, 0L);
    }
}
