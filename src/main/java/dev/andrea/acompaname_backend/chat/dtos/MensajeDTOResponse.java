package dev.andrea.acompaname_backend.chat.dtos;

import java.time.LocalDateTime;

// "mio" indica si lo ha escrito la persona que consulta el chat (para pintarlo a un lado u otro)
public record MensajeDTOResponse(Long id, Long solicitudId, Long emisorId, String emisorNombre, String texto,
        LocalDateTime fecha, boolean mio) {
}
