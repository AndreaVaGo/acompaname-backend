package dev.andrea.acompaname_backend.solicitud;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.solicitud.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilCuidadorRepository perfilCuidadorRepository;

    public SolicitudServiceImpl(SolicitudRepository repository, UsuarioRepository usuarioRepository,
            PerfilCuidadorRepository perfilCuidadorRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
    }

    private SolicitudEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SolicitudExceptionNotFound("Solicitud no encontrada. Id " + id + " no existe."));
    }

    @Override
    public List<SolicitudDTOResponse> getEntities() {
        return repository.findAll().stream()
                .map(SolicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SolicitudDTOResponse getById(Long id) {
        SolicitudEntity solicitud = findEntityById(id);
        return SolicitudMapper.toDTO(solicitud);
    }

    @Override
    public SolicitudDTOResponse storeEntity(SolicitudDTORequest dto) {
        UsuarioEntity familia = usuarioRepository.findById(dto.familiaId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.familiaId() + " no existe."));
        PerfilCuidadorEntity cuidador = perfilCuidadorRepository.findById(dto.cuidadorId())
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                        "Perfil de cuidador no encontrado. Id " + dto.cuidadorId() + " no existe."));

        SolicitudEntity solicitudToSave = SolicitudMapper.toEntity(dto, familia, cuidador);
        SolicitudEntity solicitudSave = repository.save(solicitudToSave);
        return SolicitudMapper.toDTO(solicitudSave);
    }

    @Override
    public void deleteById(Long id) {
        findEntityById(id);
        repository.deleteById(id);
    }

    @Override
    public SolicitudDTOResponse update(Long id, SolicitudDTORequest dto) {
        SolicitudEntity solicitudExistente = findEntityById(id);
        solicitudExistente.setTipoCuidado(dto.tipoCuidado());
        solicitudExistente.setNombrePaciente(dto.nombrePaciente());
        solicitudExistente.setNotas(dto.notas());
        solicitudExistente.setEdadPaciente(dto.edadPaciente());
        solicitudExistente.setFechaCuidado(dto.fechaCuidado());
        SolicitudEntity solicitudActualizada = repository.save(solicitudExistente);
        return SolicitudMapper.toDTO(solicitudActualizada);
    }

    @Override
    public SolicitudDTOResponse cambiarEstado(Long id, EstadoSolicitud nuevoEstado) {
        SolicitudEntity solicitud = findEntityById(id);
        solicitud.setEstado(nuevoEstado);
        SolicitudEntity solicitudActualizada = repository.save(solicitud);
        return SolicitudMapper.toDTO(solicitudActualizada);
    }

}