package dev.andrea.acompaname_backend.valoracion.mappers;

import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.valoracion.ValoracionEntity;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;

public class ValoracionMapper {

    public static ValoracionEntity toEntity(ValoracionDTORequest dto, SolicitudEntity solicitud) {
        ValoracionEntity valoracion = new ValoracionEntity();
        valoracion.setComentario(dto.comentario());
        valoracion.setPuntuacion(dto.puntuacion());
        valoracion.setFecha(dto.fecha());
        valoracion.setSolicitud(solicitud);
        return valoracion;
    }

    public static ValoracionDTOResponse toDTO(ValoracionEntity entity) {
        ValoracionDTOResponse dtoResponse = new ValoracionDTOResponse(
                entity.getId(),
                entity.getComentario(),
                entity.getPuntuacion(),
                entity.getFecha(),
                entity.getSolicitud().getId());
        return dtoResponse;
    }
}
