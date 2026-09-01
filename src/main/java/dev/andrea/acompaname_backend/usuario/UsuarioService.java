package dev.andrea.acompaname_backend.usuario;

import java.util.List;

import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;

public interface UsuarioService {
    public List<UsuarioEntity> getEntities();

    public UsuarioEntity getById(Long id);

    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto);

    public void deleteById(Long id);

    public UsuarioDTOResponse update(Long id, UsuarioDTORequest dto);
}
