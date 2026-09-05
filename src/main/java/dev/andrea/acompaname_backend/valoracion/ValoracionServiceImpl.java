package dev.andrea.acompaname_backend.valoracion;

import java.util.List;
import java.util.stream.Collectors;

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

    private ValoracionEntity findEntityById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ValoracionExceptionNotFound("Valoracion no encontrada. Id " + id + " no existe."));
    }

    @Override
    public List<ValoracionDTOResponse> getEntities() {
        return repository.findAll().stream()
                .map(ValoracionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ValoracionDTOResponse getById(Long id) {
        ValoracionEntity valoracion = findEntityById(id);
        return ValoracionMapper.toDTO(valoracion);
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
        findEntityById(id);
        repository.deleteById(id);
    }

    @Override
    public ValoracionDTOResponse update(Long id, ValoracionDTORequest dto) {
        ValoracionEntity valoracionExistente = findEntityById(id);
        valoracionExistente.setComentario(dto.comentario());
        valoracionExistente.setPuntuacion(dto.puntuacion());
        valoracionExistente.setFecha(dto.fecha());
        ValoracionEntity valoracionActualizada = repository.save(valoracionExistente);
        return ValoracionMapper.toDTO(valoracionActualizada);
    }

}