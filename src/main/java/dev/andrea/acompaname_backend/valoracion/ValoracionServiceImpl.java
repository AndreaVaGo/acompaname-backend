package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ValoracionServiceImpl implements ValoracionService {

    private final ValoracionRepository repository;

    public ValoracionServiceImpl(ValoracionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ValoracionEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public ValoracionEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

}
