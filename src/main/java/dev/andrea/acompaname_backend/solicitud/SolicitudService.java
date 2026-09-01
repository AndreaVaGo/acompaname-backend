package dev.andrea.acompaname_backend.solicitud;

import java.util.List;

import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;

public interface SolicitudService {
    public List<SolicitudEntity> getEntities();

    public SolicitudEntity getById(Long id);

    public SolicitudDTOResponse storeEntity(SolicitudDTORequest dto);

    public void deleteById(Long id);

    public SolicitudDTOResponse update(Long id, SolicitudDTORequest dto);
}
