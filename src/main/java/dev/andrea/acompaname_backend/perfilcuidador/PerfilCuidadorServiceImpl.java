package dev.andrea.acompaname_backend.perfilcuidador;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.perfilcuidador.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;

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
        return repository.findById(id).orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                "Perfil de cuidador no encontrado. Id " + id + " no existe."));
    }

    @Override
    public PerfilCuidadorDTOResponse storeEntity(PerfilCuidadorDTORequest dto) {
        UsuarioEntity usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new UsuarioExceptionNotFound(
                        "Usuario no encontrado. Id " + dto.usuarioId() + " no existe."));
        PerfilCuidadorEntity perfilToSave = PerfilCuidadorMapper.toEntity(dto, usuario);
        PerfilCuidadorEntity perfilSaved = repository.save(perfilToSave);
        return PerfilCuidadorMapper.toDTO(perfilSaved);
    }

    @Override
    public void deleteById(Long id) {
        getById(id);
        repository.deleteById(id);
    }

    @Override
    public PerfilCuidadorDTOResponse update(Long id, PerfilCuidadorDTORequest dto) {
        PerfilCuidadorEntity perfilCuidadorExistente = repository.findById(id)
                .orElseThrow(() -> new PerfilCuidadorExceptionNotFound(
                        "Perfil de cuidador no encontrado. Id " + id + " no existe."));
        perfilCuidadorExistente.setEspecialidad(dto.especialidad());
        perfilCuidadorExistente.setAnosExperiencia(dto.anosExperiencia());
        perfilCuidadorExistente.setTarifaHora(dto.tarifaHora());
        perfilCuidadorExistente.setBio(dto.bio());
        perfilCuidadorExistente.setTieneVehiculo(dto.tieneVehiculo());
        perfilCuidadorExistente.setDisponibleAhora(dto.disponibleAhora());
        PerfilCuidadorEntity perfilCuidadorActualizado = repository.save(perfilCuidadorExistente);
        return PerfilCuidadorMapper.toDTO(perfilCuidadorActualizado);
    }
}
