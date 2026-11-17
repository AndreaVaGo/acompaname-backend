package dev.andrea.acompaname_backend.suggestion.dtos;

import java.time.LocalDateTime;

// Lo que ve el administrador: la sugerencia con quién la escribió (nombre, email y rol)
public record AdminSugerenciaDTOResponse(
        Long id,
        String texto,
        LocalDateTime fecha,
        String nombreUsuario,
        String emailUsuario,
        String rol) {
}
