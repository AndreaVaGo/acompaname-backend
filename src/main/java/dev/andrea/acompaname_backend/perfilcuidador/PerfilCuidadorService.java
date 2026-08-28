package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

public interface PerfilCuidadorService {
    public List<PerfilCuidadorEntity> getEntities();
    public PerfilCuidadorEntity getById(Long id);

}
