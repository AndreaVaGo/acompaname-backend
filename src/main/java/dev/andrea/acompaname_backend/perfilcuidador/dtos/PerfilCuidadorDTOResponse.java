package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;

public record PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora, String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId) {
    
}
