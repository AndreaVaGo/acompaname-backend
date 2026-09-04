package dev.andrea.acompaname_backend.usuario;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.mappers.UsuarioMapper;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository repository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UsuarioEntity> getEntities() {
        return repository.findAll();
    }

    @Override
    public UsuarioEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
    }

    @Override
    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new UsuarioExceptionEmailDuplicado("El email " + dto.email() + " ya está registrado.");
        }
        Set<RoleEntity> roles = dto.rolesIds().stream()
                .map(id -> roleRepository.findById(id).orElseThrow())
                .collect(Collectors.toSet());
        UsuarioEntity usuarioToSave = UsuarioMapper.toEntity(dto, roles);
        usuarioToSave.setPassword(passwordEncoder.encode(dto.password()));
        UsuarioEntity usuarioSaved = repository.save(usuarioToSave);
        return UsuarioMapper.toDTO(usuarioSaved);
    }

    @Override
    public void deleteById(Long id) {
        getById(id);
        repository.deleteById(id);
    }

    @Override
    public UsuarioDTOResponse update(Long id, UsuarioDTORequest dto) {
        UsuarioEntity usuarioExistente = repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
        usuarioExistente.setNombre(dto.nombre());
        usuarioExistente.setEmail(dto.email());
        usuarioExistente.setTelefono(dto.telefono());
        usuarioExistente.setPassword(passwordEncoder.encode(dto.password()));
        Set<RoleEntity> roles = dto.rolesIds().stream()
                .map(rId -> roleRepository.findById(rId).orElseThrow())
                .collect(Collectors.toSet());
        usuarioExistente.setRoles(roles);
        UsuarioEntity usuarioActualizado = repository.save(usuarioExistente);
        return UsuarioMapper.toDTO(usuarioActualizado);
    }

}