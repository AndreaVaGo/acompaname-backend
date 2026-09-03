package dev.andrea.acompaname_backend.solicitud;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;

public interface SolicitudService
                extends InterfaceGenericService<SolicitudEntity, SolicitudDTORequest, SolicitudDTOResponse> {

        SolicitudDTOResponse cambiarEstado(Long id, EstadoSolicitud nuevoEstado);
}
