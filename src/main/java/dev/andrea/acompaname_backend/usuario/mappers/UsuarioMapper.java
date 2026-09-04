package dev.andrea.acompaname_backend.usuario.mappers;

import java.util.Set;
import java.util.stream.Collectors;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;

public class UsuarioMapper {

    public static UsuarioEntity toEntity(UsuarioDTORequest dto, Set<RoleEntity> roles) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setTelefono(dto.telefono());
        usuario.setPassword(dto.password());
        usuario.setRoles(roles);
        return usuario;
    }

    public static UsuarioDTOResponse toDTO(UsuarioEntity entity) {
        Set<String> rolesNombres = entity.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());
        return new UsuarioDTOResponse(entity.getId(), entity.getNombre(), entity.getEmail(), entity.getTelefono(),
                rolesNombres);
    }
    
}