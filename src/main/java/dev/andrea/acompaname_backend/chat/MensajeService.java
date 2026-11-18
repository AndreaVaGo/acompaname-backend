package dev.andrea.acompaname_backend.chat;

import java.util.List;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;

public interface MensajeService {

    // Mensajes de una solicitud. Si llega "desdeId" solo se devuelven los más nuevos que ese id
    List<MensajeDTOResponse> getMensajes(Long solicitudId, Long desdeId);

    MensajeDTOResponse enviar(Long solicitudId, MensajeDTORequest dto);
}
