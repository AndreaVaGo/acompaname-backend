package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.perfilcuidador.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;

@Service
public class PerfilCuidadorServiceImpl implements PerfilCuidadorService {

    private final PerfilCuidadorRepository repository;
    private final UsuarioRepository usuarioRepository;

    public PerfilCuidadorServiceImpl(PerfilCuidadorRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<PerfilCuidadorEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public PerfilCuidadorEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public PerfilCuidadorDTOResponse storeEntity(PerfilCuidadorDTORequest dto) {
        UsuarioEntity usuario = usuarioRepository.findById(dto.usuarioId()).orElseThrow();
        PerfilCuidadorEntity perfilToSave = PerfilCuidadorMapper.toEntity(dto, usuario);
        PerfilCuidadorEntity perfilSaved = repository.save(perfilToSave);
        return PerfilCuidadorMapper.toDTO(perfilSaved);
    }

}
