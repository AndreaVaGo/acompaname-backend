package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;

public interface PerfilCuidadorService {
    public List<PerfilCuidadorEntity> getEntities();

    public PerfilCuidadorEntity getById(Long id);

    public PerfilCuidadorDTOResponse storeEntity(PerfilCuidadorDTORequest dto);

    public void deleteById(Long id);

    public PerfilCuidadorDTOResponse update(Long id, PerfilCuidadorDTORequest dto);
}
