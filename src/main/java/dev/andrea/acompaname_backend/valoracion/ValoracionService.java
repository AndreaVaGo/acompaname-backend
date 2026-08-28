package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

public interface ValoracionService {
    
    public List<ValoracionEntity> getEntities() ;
    public ValoracionEntity getById(Long id);
}
