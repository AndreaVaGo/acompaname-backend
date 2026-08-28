package dev.andrea.acompaname_backend.solicitud;

import java.util.List;

public interface SolicitudService {
    public List<SolicitudEntity> getEntities();
    public SolicitudEntity getById(Long id);
}
