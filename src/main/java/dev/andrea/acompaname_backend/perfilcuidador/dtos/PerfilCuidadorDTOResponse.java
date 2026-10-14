package dev.andrea.acompaname_backend.perfilcuidador.dtos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
        String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
        String foto, Double valoracionMedia, Long numValoraciones, String tipoCuidado, List<String> servicios,
        String sexo, String ciudad) {

    // Constructor sin ciudad: el mismo que había antes de añadir este campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto, Double valoracionMedia, Long numValoraciones, String tipoCuidado, List<String> servicios,
            String sexo) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, valoracionMedia, numValoraciones, tipoCuidado, servicios, sexo, null);
    }

    // Constructor sin sexo: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto, Double valoracionMedia, Long numValoraciones, String tipoCuidado, List<String> servicios) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, valoracionMedia, numValoraciones, tipoCuidado, servicios, null, null);
    }

    // Constructor sin servicios: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto, Double valoracionMedia, Long numValoraciones, String tipoCuidado) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, valoracionMedia, numValoraciones, tipoCuidado, new ArrayList<>(), null, null);
    }

    // Constructor sin tipo de cuidado: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto, Double valoracionMedia, Long numValoraciones) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, valoracionMedia, numValoraciones, "ambos", new ArrayList<>(), null, null);
    }

    // Constructor sin valoraciones: el mismo que había antes de añadir esos campos
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre,
            String foto) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, foto, null, 0L, "ambos", new ArrayList<>(), null, null);
    }

    // Constructor sin foto: el mismo que había antes de añadir ese campo
    public PerfilCuidadorDTOResponse(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, Long usuarioId, String usuarioNombre) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuarioId,
                usuarioNombre, null, null, 0L, "ambos", new ArrayList<>(), null, null);
    }
}