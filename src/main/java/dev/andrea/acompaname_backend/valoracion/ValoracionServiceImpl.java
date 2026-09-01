package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;
import dev.andrea.acompaname_backend.valoracion.mappers.ValoracionMapper;

@Service
public class ValoracionServiceImpl implements ValoracionService {

    private final ValoracionRepository repository;
    private final SolicitudRepository solicitudRepository;

    public ValoracionServiceImpl(ValoracionRepository repository, SolicitudRepository solicitudRepository) {
        this.repository = repository;
        this.solicitudRepository = solicitudRepository;
    }

    @Override
    public List<ValoracionEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public ValoracionEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public ValoracionDTOResponse storeEntity(ValoracionDTORequest dto) {
        SolicitudEntity solicitud = solicitudRepository.findById(dto.solicitudId()).orElseThrow();
        ValoracionEntity valoracionToSave = ValoracionMapper.toEntity(dto, solicitud);
        ValoracionEntity valoracionSave = repository.save(valoracionToSave);
        return ValoracionMapper.toDTO(valoracionSave);
    }

}