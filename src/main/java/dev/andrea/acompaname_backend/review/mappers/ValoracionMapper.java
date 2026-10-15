package dev.andrea.acompaname_backend.review.mappers;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.review.ValoracionEntity;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTOResponse;
import dev.andrea.acompaname_backend.review.dtos.ValoracionPublicaDTOResponse;

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

    public static ValoracionPublicaDTOResponse toPublicaDTO(ValoracionEntity entity) {
        return new ValoracionPublicaDTOResponse(
                entity.getPuntuacion(),
                entity.getComentario(),
                entity.getFecha());
    }
}