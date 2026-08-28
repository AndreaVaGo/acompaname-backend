package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PerfilCuidadorServiceImpl implements PerfilCuidadorService{

    private final PerfilCuidadorRepository repository;

    public PerfilCuidadorServiceImpl(PerfilCuidadorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PerfilCuidadorEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public PerfilCuidadorEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    
    
}
