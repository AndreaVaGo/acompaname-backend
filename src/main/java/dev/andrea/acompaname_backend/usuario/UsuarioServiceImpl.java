package dev.andrea.acompaname_backend.usuario;

import java.util.List;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.usuario.mappers.UsuarioMapper;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioServiceImpl(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UsuarioEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public UsuarioEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto) {
        UsuarioEntity usuarioToSave = UsuarioMapper.toEntity(dto);
        UsuarioEntity usuarioSaved = repository.save(usuarioToSave);
        return UsuarioMapper.toDTO(usuarioSaved);
    }

}
