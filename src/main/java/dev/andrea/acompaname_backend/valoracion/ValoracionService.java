package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;

public interface ValoracionService {

    public List<ValoracionEntity> getEntities();

    public ValoracionEntity getById(Long id);

    public ValoracionDTOResponse storeEntity(ValoracionDTORequest dto);

    public void deleteById(Long id);

    public ValoracionDTOResponse update(Long id, ValoracionDTORequest dto);
}
