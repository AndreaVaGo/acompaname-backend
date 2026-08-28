package dev.andrea.acompaname_backend.usuario.mappers;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTORequest;
import dev.andrea.acompaname_backend.usuario.dtos.UsuarioDTOResponse;

public class UsuarioMapper {

    public static UsuarioEntity toEntity(UsuarioDTORequest dtoRequest) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(dtoRequest.nombre());
        usuario.setEmail(dtoRequest.email());
        usuario.setTelefono(dtoRequest.telefono());
        usuario.setPassword(dtoRequest.password());
        usuario.setRol(dtoRequest.rol());
        return usuario;
    }

    public static UsuarioDTOResponse toDTO(UsuarioEntity entity) {
        UsuarioDTOResponse dtoResponse = new UsuarioDTOResponse(entity.getId(), entity.getNombre(), entity.getEmail(), entity.getTelefono(), entity.getRol());
        return dtoResponse;
    }
}
