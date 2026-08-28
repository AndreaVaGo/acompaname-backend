package dev.andrea.acompaname_backend.usuario;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UsuarioService{

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
    
}
