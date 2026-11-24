package dev.andrea.acompaname_backend.fichaje.dtos;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.fichaje.FichajeEntity;
import dev.andrea.acompaname_backend.fichaje.ResultadoUbicacion;
import dev.andrea.acompaname_backend.fichaje.TipoFichaje;

// Lo que ven la familia y la persona cuidadora: cuándo y si estaba en el lugar (nunca coordenadas)
public record FichajeDTOResponse(
        Long id,
        TipoFichaje tipo,
        LocalDateTime momento,
        ResultadoUbicacion ubicacion) {

    public static FichajeDTOResponse de(FichajeEntity fichaje) {
        return new FichajeDTOResponse(fichaje.getId(), fichaje.getTipo(), fichaje.getMomento(),
                fichaje.getUbicacion());
    }
}
