package dev.andrea.acompaname_backend.payment;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.payment.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.payment.mappers.PagoMapper;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository repository;

    // Los avisos por correo son opcionales: si no hay (por ejemplo, en los tests que no los usan),
    // el pago funciona igual, solo que sin mandar correos
    private AvisosDePago avisos;

    public PagoServiceImpl(PagoRepository repository) {
        this.repository = repository;
    }

    @Autowired(required = false)
    public void setAvisos(AvisosDePago avisos) {
        this.avisos = avisos;
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

    // Solo la familia es quien paga
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
        // La base de datos devuelve solo los pagos del usuario logueado (como familia o como cuidador)
        return repository.findByParticipante(emailLogueado()).stream()
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
    public PagoDTOResponse marcarComoPagado(Long id) {
        PagoEntity pago = findEntityById(id);
        verificarFamilia(pago.getSolicitud());
        verificarPendiente(pago);
        pago.setEstado(EstadoPago.COMPLETADO);
        PagoEntity pagoActualizado = repository.save(pago);
        // Ya está pagado: se avisa a la persona cuidadora (si el correo falla, el pago sigue hecho)
        if (avisos != null) {
            avisos.avisarPagoRecibido(pagoActualizado);
        }
        return PagoMapper.toDTO(pagoActualizado);
    }
}