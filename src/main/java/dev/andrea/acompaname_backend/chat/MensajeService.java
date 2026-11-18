package dev.andrea.acompaname_backend.chat;

import java.util.List;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.chat.dtos.MensajesNoLeidosDTOResponse;

public interface MensajeService {

    // Mensajes de una solicitud. Si llega "desdeId" solo se devuelven los más nuevos que ese id
    List<MensajeDTOResponse> getMensajes(Long solicitudId, Long desdeId);

    MensajeDTOResponse enviar(Long solicitudId, MensajeDTORequest dto);

    // Cuántos mensajes sin leer tiene la persona que consulta, en total y por solicitud
    MensajesNoLeidosDTOResponse getNoLeidos();
}
