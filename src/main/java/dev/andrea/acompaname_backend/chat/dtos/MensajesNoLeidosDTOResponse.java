package dev.andrea.acompaname_backend.chat.dtos;

import java.util.Map;

// "total" son todos los mensajes sin leer de la persona; "porSolicitud" los reparte por id de solicitud
public record MensajesNoLeidosDTOResponse(long total, Map<Long, Long> porSolicitud) {
}
