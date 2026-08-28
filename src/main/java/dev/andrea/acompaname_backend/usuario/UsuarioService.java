package dev.andrea.acompaname_backend.usuario;

import java.util.List;

public interface UsuarioService {
    public List<UsuarioEntity> getEntities();
    public UsuarioEntity getById(Long id);
}
 