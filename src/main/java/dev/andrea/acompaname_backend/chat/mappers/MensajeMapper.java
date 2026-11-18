package dev.andrea.acompaname_backend.chat.mappers;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.chat.MensajeEntity;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class MensajeMapper {

    public static MensajeEntity toEntity(MensajeDTORequest dto, SolicitudEntity solicitud, UsuarioEntity emisor) {
        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setSolicitud(solicitud);
        mensaje.setEmisor(emisor);
        // Se quitan los espacios sobrantes de los lados
        mensaje.setTexto(dto.texto().trim());
        // La fecha la pone el servidor, no quien escribe
        mensaje.setFecha(LocalDateTime.now());
        // Un mensaje nuevo siempre empieza sin leer
        mensaje.setLeido(false);
        return mensaje;
    }

    public static MensajeDTOResponse toDTO(MensajeEntity entity, String emailLogueado) {
        return new MensajeDTOResponse(
                entity.getId(),
                entity.getSolicitud().getId(),
                entity.getEmisor().getId(),
                entity.getEmisor().getNombre(),
                entity.getTexto(),
                entity.getFecha(),
                entity.getEmisor().getEmail().equals(emailLogueado));
    }
}
