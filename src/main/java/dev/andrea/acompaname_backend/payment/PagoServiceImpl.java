package dev.andrea.acompaname_backend.payment;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.payment.mappers.PagoMapper;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;

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

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private boolean esParticipante(SolicitudEntity solicitud, String email) {
        return solicitud.getFamilia().getEmail().equals(email)
                || solicitud.getCuidador().getUsuario().getEmail().equals(email);
    }

    // Solo la familia y el cuidador de la solicitud pueden ver su pago
    private void verificarParticipante(SolicitudEntity solicitud) {
        if (!esParticipante(solicitud, emailLogueado())) {
            throw new PagoExceptionAccesoDenegado("No tiene permiso para acceder a este pago");
        }
    }

    // Solo la familia es quien paga: crea, cambia, borra y paga el pago
    private void verificarFamilia(SolicitudEntity solicitud) {
        if (!solicitud.getFamilia().getEmail().equals(emailLogueado())) {
            throw new PagoExceptionAccesoDenegado("Solo la familia de la solicitud puede gestionar su pago");
        }
    }

    // Un pago que ya no está pendiente (pagado o cancelado) no se puede tocar
    private void verificarPendiente(PagoEntity pago) {
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new PagoExceptionConflicto("Este pago ya no está pendiente");
        }
    }

    @Override
    public List<PagoDTOResponse> getEntities() {
        String email = emailLogueado();
        return repository.findAll().stream()
                .filter(pago -> esParticipante(pago.getSolicitud(), email))
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PagoDTOResponse getById(Long id) {
        PagoEntity pago = findEntityById(id);
        verificarParticipante(pago.getSolicitud());
        return PagoMapper.toDTO(pago);
    }

    @Transactional
    @Override
    public PagoDTOResponse storeEntity(PagoDTORequest dto) {
        SolicitudEntity solicitud = solicitudRepository.findById(dto.solicitudId())
                .orElseThrow(() -> new SolicitudExceptionNotFound(
                        "Solicitud no encontrada. Id " + dto.solicitudId() + " no existe."));
        verificarFamilia(solicitud);

        boolean sePuedePagar = solicitud.getEstado() == EstadoSolicitud.ACEPTADA
                || solicitud.getEstado() == EstadoSolicitud.COMPLETADA;
        if (!sePuedePagar) {
            throw new PagoExceptionConflicto("Solo se puede crear el pago de una solicitud aceptada o completada");
        }
        if (repository.existsBySolicitudId(solicitud.getId())) {
            throw new PagoExceptionConflicto("Esta solicitud ya tiene un pago");
        }

        PagoEntity pagoToSave = PagoMapper.toEntity(dto, solicitud);
        PagoEntity pagoSaved = repository.save(pagoToSave);
        return PagoMapper.toDTO(pagoSaved);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        PagoEntity pago = findEntityById(id);
        verificarFamilia(pago.getSolicitud());
        verificarPendiente(pago);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public PagoDTOResponse update(Long id, PagoDTORequest dto) {
        PagoEntity pagoExistente = findEntityById(id);
        verificarFamilia(pagoExistente.getSolicitud());
        verificarPendiente(pagoExistente);
        pagoExistente.setImporte(dto.importe());
        PagoEntity pagoActualizado = repository.save(pagoExistente);
        return PagoMapper.toDTO(pagoActualizado);
    }

    @Transactional
    @Override
    public PagoDTOResponse marcarComoPagado(Long id) {
        PagoEntity pago = findEntityById(id);
        verificarFamilia(pago.getSolicitud());
        verificarPendiente(pago);
        pago.setEstado(EstadoPago.COMPLETADO);
        PagoEntity pagoActualizado = repository.save(pago);
        return PagoMapper.toDTO(pagoActualizado);
    }
}