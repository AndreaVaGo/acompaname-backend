package dev.andrea.acompaname_backend.generics;

import java.util.List;

public interface InterfaceGenericService<Entity, DTORequest, DTOResponse> {

    List<Entity> getEntities();

    Entity getById(Long id);

    DTOResponse storeEntity(DTORequest dto);

    void deleteById(Long id);

    DTOResponse update(Long id, DTORequest dto);

}
