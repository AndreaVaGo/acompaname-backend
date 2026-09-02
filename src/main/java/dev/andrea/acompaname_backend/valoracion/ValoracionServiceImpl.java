package dev.andrea.acompaname_backend.valoracion;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionNotFound;
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
        return repository.findById(id).orElseThrow(
                () -> new ValoracionExceptionNotFound("Valoracion no encontrada. Id " + id + " no existe."));
    }

    @Override
    public ValoracionDTOResponse storeEntity(ValoracionDTORequest dto) {
        SolicitudEntity solicitud = solicitudRepository.findById(dto.solicitudId())
                .orElseThrow(() -> new SolicitudExceptionNotFound(
                        "Solicitud no encontrada. Id " + dto.solicitudId() + " no existe."));
        ValoracionEntity valoracionToSave = ValoracionMapper.toEntity(dto, solicitud);
        ValoracionEntity valoracionSave = repository.save(valoracionToSave);
        return ValoracionMapper.toDTO(valoracionSave);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public ValoracionDTOResponse update(Long id, ValoracionDTORequest dto) {
        ValoracionEntity valoracionExistente = repository.findById(id).orElseThrow(
                () -> new ValoracionExceptionNotFound("Valoracion no encontrada. Id " + id + " no existe."));
        valoracionExistente.setComentario(dto.comentario());
        valoracionExistente.setPuntuacion(dto.puntuacion());
        valoracionExistente.setFecha(dto.fecha());
        ValoracionEntity valoracionActualizada = repository.save(valoracionExistente);
        return ValoracionMapper.toDTO(valoracionActualizada);
    }

}