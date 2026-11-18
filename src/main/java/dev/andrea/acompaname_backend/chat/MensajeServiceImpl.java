package dev.andrea.acompaname_backend.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.chat.exceptions.ChatExceptionConflicto;
import dev.andrea.acompaname_backend.chat.mappers.MensajeMapper;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

@Service
public class MensajeServiceImpl implements MensajeService {

    private final MensajeRepository repository;
    private final SolicitudRepository solicitudRepository;

    public MensajeServiceImpl(MensajeRepository repository, SolicitudRepository solicitudRepository) {
        this.repository = repository;
        this.solicitudRepository = solicitudRepository;
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private SolicitudEntity buscarSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId).orElseThrow(
                () -> new SolicitudExceptionNotFound("Solicitud no encontrada. Id " + solicitudId + " no existe."));
    }

    // Solo hablan entre sí la familia de la solicitud y la persona cuidadora a la que se la pidió.
    // Devuelve a la persona que está dentro del chat
    private UsuarioEntity participanteLogueado(SolicitudEntity solicitud) {
        String email = emailLogueado();
        if (solicitud.getFamilia().getEmail().equals(email)) {
            return solicitud.getFamilia();
        }
        if (solicitud.getCuidador().getUsuario().getEmail().equals(email)) {
            return solicitud.getCuidador().getUsuario();
        }
        throw new ChatExceptionAccesoDenegado("No tiene permiso para ver el chat de esta solicitud");
    }

    @Transactional(readOnly = true)
    @Override
    public List<MensajeDTOResponse> getMensajes(Long solicitudId, Long desdeId) {
        SolicitudEntity solicitud = buscarSolicitud(solicitudId);
        participanteLogueado(solicitud);

        List<MensajeEntity> mensajes;
        if (desdeId == null) {
            mensajes = repository.findBySolicitudIdOrderByIdAsc(solicitudId);
        } else {
            mensajes = repository.findBySolicitudIdAndIdGreaterThanOrderByIdAsc(solicitudId, desdeId);
        }

        String email = emailLogueado();
        return mensajes.stream()
                .map(mensaje -> MensajeMapper.toDTO(mensaje, email))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public MensajeDTOResponse enviar(Long solicitudId, MensajeDTORequest dto) {
        SolicitudEntity solicitud = buscarSolicitud(solicitudId);
        UsuarioEntity emisor = participanteLogueado(solicitud);

        // En una solicitud rechazada la conversación se queda cerrada (se puede leer, pero no escribir)
        if (solicitud.getEstado() == EstadoSolicitud.RECHAZADA) {
            throw new ChatExceptionConflicto("No se puede escribir en una solicitud rechazada");
        }

        MensajeEntity guardado = repository.save(MensajeMapper.toEntity(dto, solicitud, emisor));
        return MensajeMapper.toDTO(guardado, emisor.getEmail());
    }
}
