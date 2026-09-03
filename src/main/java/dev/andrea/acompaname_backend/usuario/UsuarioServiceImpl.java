package dev.andrea.acompaname_backend.usuario;

import java.util.List;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;
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
        return repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
    }

    @Override
    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto) {
        UsuarioEntity usuarioToSave = UsuarioMapper.toEntity(dto);
        UsuarioEntity usuarioSaved = repository.save(usuarioToSave);
        return UsuarioMapper.toDTO(usuarioSaved);
    }

    @Override
    public void deleteById(Long id) {
        getById(id);
        repository.deleteById(id);
    }

    @Override
    public UsuarioDTOResponse update(Long id, UsuarioDTORequest dto) {
        UsuarioEntity usuarioExistente = repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
        usuarioExistente.setNombre(dto.nombre());
        usuarioExistente.setEmail(dto.email());
        usuarioExistente.setTelefono(dto.telefono());
        usuarioExistente.setPassword(dto.password());
        usuarioExistente.setRol(dto.rol());
        UsuarioEntity usuarioActualizado = repository.save(usuarioExistente);
        return UsuarioMapper.toDTO(usuarioActualizado);
    }

}
