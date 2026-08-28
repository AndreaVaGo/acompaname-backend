package dev.andrea.acompaname_backend.solicitud;

import java.util.List;


import org.springframework.stereotype.Service;

@Service
public class SolicitudServiceImpl implements SolicitudService{

    private final SolicitudRepository repository;

    

    public SolicitudServiceImpl(SolicitudRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SolicitudEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public SolicitudEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
       
    }
    
}
