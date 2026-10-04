package dev.andrea.acompaname_backend.pago;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.pago.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.pago.mappers.PagoMapper;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository repository;
    private final SolicitudRepository solicitudRepository;

    public PagoServiceImpl(PagoRepository repository, SolicitudRepository solicitudRepository) {
        this.repository = repository;
        this.solicitudRepository = solicitudRepository;
    }

    private PagoEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PagoExceptionNotFound("Pago no encontrado. Id " + id + " no existe."));
    }

    @Override
    public List<PagoDTOResponse> getEntities() {
        return repository.findAll().stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PagoDTOResponse getById(Long id) {
        PagoEntity pago = findEntityById(id);
        return PagoMapper.toDTO(pago);
    }

    @Transactional
    @Override
    public PagoDTOResponse storeEntity(PagoDTORequest dto) {
        SolicitudEntity solicitud = solicitudRepository.findById(dto.solicitudId())
                .orElseThrow(() -> new SolicitudExceptionNotFound(
                        "Solicitud no encontrada. Id " + dto.solicitudId() + " no existe."));
        PagoEntity pagoToSave = PagoMapper.toEntity(dto, solicitud);
        PagoEntity pagoSaved = repository.save(pagoToSave);
        return PagoMapper.toDTO(pagoSaved);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        findEntityById(id);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public PagoDTOResponse update(Long id, PagoDTORequest dto) {
        PagoEntity pagoExistente = findEntityById(id);
        pagoExistente.setImporte(dto.importe());
        PagoEntity pagoActualizado = repository.save(pagoExistente);
        return PagoMapper.toDTO(pagoActualizado);
    }

    @Transactional
    @Override
    public PagoDTOResponse marcarComoPagado(Long id) {
        PagoEntity pago = findEntityById(id);
        pago.setEstado(EstadoPago.COMPLETADO);
        PagoEntity pagoActualizado = repository.save(pago);
        return PagoMapper.toDTO(pagoActualizado);
    }
}