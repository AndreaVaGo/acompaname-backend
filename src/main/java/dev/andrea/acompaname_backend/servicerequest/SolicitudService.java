package dev.andrea.acompaname_backend.servicerequest;

import java.time.LocalDate;
import java.util.List;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;

public interface SolicitudService
                extends InterfaceGenericService<SolicitudEntity, SolicitudDTORequest, SolicitudDTOResponse> {

        SolicitudDTOResponse cambiarEstado(Long id, EstadoSolicitud nuevoEstado);

        List<SolicitudDTOResponse> getMisSolicitudes();

        // Ids de los cuidadores que ya tienen un servicio aceptado que se cruza con esas fechas
        List<Long> getCuidadoresOcupados(LocalDate inicio, LocalDate fin);
}
