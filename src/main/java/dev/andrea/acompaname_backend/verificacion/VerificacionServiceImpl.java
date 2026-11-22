package dev.andrea.acompaname_backend.verificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.verificacion.dtos.AdminVerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificadosDTOResponse;

@Service
public class VerificacionServiceImpl implements VerificacionService {

    private final VerificacionRepository repository;
    private final PerfilCuidadorRepository perfilRepository;

    public VerificacionServiceImpl(VerificacionRepository repository, PerfilCuidadorRepository perfilRepository) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private VerificacionDTOResponse aDTO(VerificacionEntity v) {
        return new VerificacionDTOResponse(v.getId(), v.getTipo(), v.getEstado(), v.getReferencia(),
                v.getSolicitadaEn(), v.getRevisadaEn(), v.getMotivoRechazo());
    }

    private AdminVerificacionDTOResponse aDTOAdmin(VerificacionEntity v) {
        return new AdminVerificacionDTOResponse(v.getId(), v.getCuidador().getId(),
                v.getCuidador().getUsuario().getNombre(), v.getTipo(), v.getEstado(), v.getReferencia(),
                v.getSolicitadaEn(), v.getRevisadaEn(), v.getMotivoRechazo());
    }

    @Override
    public List<VerificacionDTOResponse> listarMias() {
        List<VerificacionDTOResponse> respuesta = new ArrayList<>();
        // Sin perfil todavía no puede tener verificaciones: la lista sale vacía
        PerfilCuidadorEntity perfil = perfilRepository.findByUsuarioEmail(emailLogueado()).orElse(null);
        if (perfil == null) {
            return respuesta;
        }
        for (VerificacionEntity v : repository.findByCuidadorId(perfil.getId())) {
            respuesta.add(aDTO(v));
        }
        return respuesta;
    }

    @Transactional
    @Override
    public VerificacionDTOResponse enviar(TipoVerificacion tipo, String referencia) {
        PerfilCuidadorEntity perfil = perfilRepository.findByUsuarioEmail(emailLogueado()).orElseThrow(
                () -> new PerfilCuidadorExceptionNotFound("Primero tienes que crear tu perfil de cuidador"));

        VerificacionEntity existente = repository.findByCuidadorIdAndTipo(perfil.getId(), tipo).orElse(null);
        if (existente != null && existente.getEstado() == EstadoVerificacion.VERIFICADO) {
            throw new VerificacionException("Esta verificación ya está comprobada");
        }

        // Si ya había una pendiente o rechazada se reaprovecha: vuelve a quedar pendiente de revisar
        VerificacionEntity v = existente != null ? existente
                : VerificacionEntity.builder().cuidador(perfil).tipo(tipo).build();
        v.setEstado(EstadoVerificacion.PENDIENTE);
        v.setReferencia(referencia.trim());
        v.setSolicitadaEn(LocalDateTime.now());
        v.setRevisadaEn(null);
        v.setRevisadaPor(null);
        v.setMotivoRechazo(null);
        return aDTO(repository.save(v));
    }

    // Una sola consulta: se recorren las verificaciones y se marca qué tiene verificado cada cuidador
    @Override
    public List<VerificadosDTOResponse> publicas() {
        Map<Long, boolean[]> porCuidador = new LinkedHashMap<>();
        for (VerificacionEntity v : repository.findByEstadoOrderBySolicitadaEnAsc(EstadoVerificacion.VERIFICADO)) {
            boolean[] marcas = porCuidador.computeIfAbsent(v.getCuidador().getId(), id -> new boolean[2]);
            if (v.getTipo() == TipoVerificacion.TITULACION) {
                marcas[0] = true;
            } else {
                marcas[1] = true;
            }
        }
        List<VerificadosDTOResponse> respuesta = new ArrayList<>();
        for (Map.Entry<Long, boolean[]> fila : porCuidador.entrySet()) {
            respuesta.add(new VerificadosDTOResponse(fila.getKey(), fila.getValue()[0], fila.getValue()[1]));
        }
        return respuesta;
    }

    @Override
    public List<AdminVerificacionDTOResponse> listarParaAdmin(EstadoVerificacion estado) {
        List<VerificacionEntity> filas = estado == null ? repository.findAllByOrderBySolicitadaEnAsc()
                : repository.findByEstadoOrderBySolicitadaEnAsc(estado);
        List<AdminVerificacionDTOResponse> respuesta = new ArrayList<>();
        for (VerificacionEntity v : filas) {
            respuesta.add(aDTOAdmin(v));
        }
        return respuesta;
    }

    @Transactional
    @Override
    public AdminVerificacionDTOResponse resolver(Long id, boolean aprobar, String motivo) {
        VerificacionEntity v = repository.findById(id)
                .orElseThrow(() -> new VerificacionException("La verificación no existe"));
        if (v.getEstado() != EstadoVerificacion.PENDIENTE) {
            throw new VerificacionException("Esta verificación ya está revisada");
        }
        v.setEstado(aprobar ? EstadoVerificacion.VERIFICADO : EstadoVerificacion.RECHAZADO);
        v.setRevisadaEn(LocalDateTime.now());
        v.setRevisadaPor(emailLogueado());
        // El motivo solo tiene sentido al rechazar
        boolean hayMotivo = !aprobar && motivo != null && !motivo.isBlank();
        v.setMotivoRechazo(hayMotivo ? motivo.trim() : null);
        return aDTOAdmin(repository.save(v));
    }
}
