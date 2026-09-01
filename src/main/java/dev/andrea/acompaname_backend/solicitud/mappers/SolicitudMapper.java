package dev.andrea.acompaname_backend.solicitud.mappers;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;

public class SolicitudMapper {

    public static SolicitudEntity toEntity(SolicitudDTORequest dto, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTipoCuidado(dto.tipoCuidado());
        solicitud.setNombrePaciente(dto.nombrePaciente());
        solicitud.setNotas(dto.notas());
        solicitud.setEdadPaciente(dto.edadPaciente());
        solicitud.setFechaCuidado(dto.fechaCuidado());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        return solicitud;
    }

    public static SolicitudDTOResponse toDTO(SolicitudEntity entity) {
        SolicitudDTOResponse dtoResponse = new SolicitudDTOResponse(
                entity.getId(),
                entity.getTipoCuidado(),
                entity.getNombrePaciente(),
                entity.getNotas(),
                entity.getEdadPaciente(),
                entity.getFechaCuidado(),
                entity.getEstado(),
                entity.getFamilia().getId(),
                entity.getCuidador().getId());
        return dtoResponse;
    }
}
