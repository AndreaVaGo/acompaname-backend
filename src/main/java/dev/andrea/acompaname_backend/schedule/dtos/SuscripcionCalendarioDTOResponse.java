package dev.andrea.acompaname_backend.schedule.dtos;

import java.time.LocalDateTime;

// Cómo está la vinculación. La dirección NO se devuelve nunca: es secreta
public record SuscripcionCalendarioDTOResponse(boolean vinculado, LocalDateTime ultimaSincronizacion,
        String ultimoError) {

    public static SuscripcionCalendarioDTOResponse noVinculado() {
        return new SuscripcionCalendarioDTOResponse(false, null, null);
    }
}
