package dev.andrea.acompaname_backend.servicerequest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.mappers.PagoMapper;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;
    private final PagoRepository pagoRepository;

    public SolicitudServiceImpl(SolicitudRepository repository, UsuarioRepository usuarioRepository,
            PerfilCuidadorRepository perfilCuidadorRepository, PagoRepository pagoRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
        this.pagoRepository = pagoRepository;
    }

    private SolicitudEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SolicitudExceptionNotFound("Solicitud no encontrada. Id " + id + " no existe."));
    }

    private String emailLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void verificarPropietario(SolicitudEntity solicitud) {
        String emailLogueado = emailLogueado();
        if (!solicitud.getFamilia().getEmail().equals(emailLogueado)
                && !solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado)) {
            throw new SolicitudExceptionAccesoDenegado("No tiene permiso para acceder a esta solicitud");
        }
    }

    // Solo el cuidador de la solicitud puede aceptarla, rechazarla o completarla
    private void verificarCuidador(SolicitudEntity solicitud) {
        if (!solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado())) {
            throw new SolicitudExceptionAccesoDenegado("Solo el cuidador de la solicitud puede cambiar su estado");
        }
    }

    // El cuidador solo ve la dirección cuando ha aceptado la solicitud; la familia la ve siempre
    private SolicitudDTOResponse aDTO(SolicitudEntity solicitud) {
        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);
        boolean esCuidador = solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado());
        boolean aceptada = solicitud.getEstado() == EstadoSolicitud.ACEPTADA
                || solicitud.getEstado() == EstadoSolicitud.COMPLETADA;
        if (!esCuidador || aceptada) {
            return dto;
        }
        return new SolicitudDTOResponse(dto.id(), dto.tipoCuidado(), dto.nombrePaciente(), dto.notas(),
                dto.edadPaciente(), dto.fechaCuidado(), dto.fechaFin(), dto.estado(), dto.familiaId(),
                dto.cuidadorId(), dto.familiaNombre(), dto.cuidadorNombre(), dto.necesidades(), null);
    }

    // La fecha de fin es opcional, pero si viene no puede ser anterior a la de inicio
    private void verificarFechas(SolicitudDTORequest dto) {
        if (dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaCuidado())) {
            throw new SolicitudExceptionFechaInvalida("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    @Override
    public List<SolicitudDTOResponse> getEntities() {
        // Cada usuario solo ve sus solicitudes, no las de los demás
        return getMisSolicitudes();
    }

    @Override
    public SolicitudDTOResponse getById(Long id) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarPropietario(solicitud);
        return aDTO(solicitud);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse storeEntity(SolicitudDTORequest dto) {
        verificarFechas(dto);
        UsuarioEntity familia = usuarioRepository.findById(dto.familiaId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.familiaId() + " no existe."));
        PerfilCuidadorEntity cuidador = perfilCuidadorRepository.findById(dto.cuidadorId())
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                        "Perfil de cuidador no encontrado. Id " + dto.cuidadorId() + " no existe."));

        if (!familia.getEmail().equals(emailLogueado())) {
            throw new SolicitudExceptionAccesoDenegado("No puede crear solicitudes en nombre de otra familia");
        }

        SolicitudEntity solicitudToSave = SolicitudMapper.toEntity(dto, familia, cuidador);
        SolicitudEntity solicitudSave = repository.save(solicitudToSave);
        return SolicitudMapper.toDTO(solicitudSave);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarPropietario(solicitud);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse update(Long id, SolicitudDTORequest dto) {
        verificarFechas(dto);
        SolicitudEntity solicitudExistente = findEntityById(id);
        verificarPropietario(solicitudExistente);
        solicitudExistente.setTipoCuidado(dto.tipoCuidado());
        solicitudExistente.setNombrePaciente(dto.nombrePaciente());
        solicitudExistente.setNotas(dto.notas());
        solicitudExistente.setEdadPaciente(dto.edadPaciente());
        solicitudExistente.setFechaCuidado(dto.fechaCuidado());
        solicitudExistente.setFechaFin(dto.fechaFin());
        // Si no vienen necesidades se mantienen las que había; una lista vacía las borra
        if (dto.necesidades() != null) {
            solicitudExistente.setNecesidades(String.join(",", dto.necesidades()));
        }
        // La dirección se actualiza siempre: si no llega, se borra
        solicitudExistente.setDireccion(dto.direccion());
        SolicitudEntity solicitudActualizada = repository.save(solicitudExistente);
        return aDTO(solicitudActualizada);
    }

    @Transactional
    @Override
    public SolicitudDTOResponse cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        SolicitudEntity solicitud = findEntityById(id);
        verificarCuidador(solicitud);
        boolean seAcepta = nuevoEstado == EstadoSolicitud.ACEPTADA
                && solicitud.getEstado() != EstadoSolicitud.ACEPTADA;
        solicitud.setEstado(nuevoEstado);
        SolicitudEntity solicitudActualizada = repository.save(solicitud);
        if (seAcepta && !pagoRepository.existsBySolicitudId(solicitud.getId())) {
            // Al aceptar la solicitud se genera su pago pendiente, con la tarifa del cuidador
            PagoDTORequest pago = new PagoDTORequest(solicitud.getCuidador().getTarifaHora(), solicitud.getId());
            pagoRepository.save(PagoMapper.toEntity(pago, solicitud));
        }
        return aDTO(solicitudActualizada);
    }

    @Override
    public List<SolicitudDTOResponse> getMisSolicitudes() {
        String emailLogueado = emailLogueado();

        return repository.findAll().stream()
                .filter(solicitud -> solicitud.getFamilia().getEmail().equals(emailLogueado)
                        || solicitud.getCuidador().getUsuario().getEmail().equals(emailLogueado))
                .map(this::aDTO)
                .collect(Collectors.toList());
    }
}