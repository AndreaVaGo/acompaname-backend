package dev.andrea.acompaname_backend.usuario;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.mappers.UsuarioMapper;
import org.springframework.transaction.annotation.Transactional;

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

    private UsuarioEntity findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UsuarioExceptionNotFound("Usuario no encontrado. Id " + id + " no existe."));
    }

    private void verificarPropietario(UsuarioEntity usuario) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailLogueado = auth.getName();
        if (!usuario.getEmail().equals(emailLogueado)) {
            throw new UsuarioExceptionAccesoDenegado("No tiene permiso para acceder a este usuario");
        }
    }

    @Override
    public List<UsuarioDTOResponse> getEntities() {
        return repository.findAll().stream()
                .map(UsuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTOResponse getById(Long id) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    @Transactional
    @Override
    public UsuarioDTOResponse storeEntity(UsuarioDTORequest dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new UsuarioExceptionEmailDuplicado(
                    "No ha sido posible completar el registro con los datos proporcionados.");
        }
        Set<RoleEntity> roles = dto.rolesIds().stream()
                .map(id -> roleRepository.findById(id).orElseThrow())
                .collect(Collectors.toSet());
        UsuarioEntity usuarioToSave = UsuarioMapper.toEntity(dto, roles);
        usuarioToSave.setPassword(passwordEncoder.encode(dto.password()));
        UsuarioEntity usuarioSaved = repository.save(usuarioToSave);
        return UsuarioMapper.toDTO(usuarioSaved);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        UsuarioEntity usuario = findEntityById(id);
        verificarPropietario(usuario);
        repository.deleteById(id);
    }

    @Transactional
    @Override
    public UsuarioDTOResponse update(Long id, UsuarioDTORequest dto) {
        UsuarioEntity usuarioExistente = findEntityById(id);
        verificarPropietario(usuarioExistente);
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