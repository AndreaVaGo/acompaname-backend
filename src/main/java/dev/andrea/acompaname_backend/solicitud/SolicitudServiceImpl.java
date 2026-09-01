package dev.andrea.acompaname_backend.solicitud;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;

import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.solicitud.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;

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

    @Override
    public List<SolicitudEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public SolicitudEntity getById(Long id) {
        return repository.findById(id).orElseThrow();

    }

    @Override
    public SolicitudDTOResponse storeEntity(SolicitudDTORequest dto) {
        UsuarioEntity familia = usuarioRepository.findById(dto.familiaId()).orElseThrow();
        PerfilCuidadorEntity cuidador = perfilCuidadorRepository.findById(dto.cuidadorId()).orElseThrow();

        SolicitudEntity solicitudToSave = SolicitudMapper.toEntity(dto, familia, cuidador);
        SolicitudEntity solicitudSave = repository.save(solicitudToSave);
        return SolicitudMapper.toDTO(solicitudSave);
    }
}