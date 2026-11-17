package dev.andrea.acompaname_backend.suggestion.mappers;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.suggestion.SugerenciaEntity;
import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SugerenciaMapper {

    public static SugerenciaEntity toEntity(SugerenciaDTORequest dto, UsuarioEntity usuario) {
        SugerenciaEntity sugerencia = new SugerenciaEntity();
        // Se quitan los espacios de los lados; la fecha la pone el servidor (ahora)
        sugerencia.setTexto(dto.texto().trim());
        sugerencia.setFecha(LocalDateTime.now());
        sugerencia.setUsuario(usuario);
        return sugerencia;
    }

    public static SugerenciaDTOResponse toDTO(SugerenciaEntity entity) {
        return new SugerenciaDTOResponse(entity.getId(), entity.getTexto(), entity.getFecha());
    }

    public static AdminSugerenciaDTOResponse toAdminDTO(SugerenciaEntity entity) {
        UsuarioEntity usuario = entity.getUsuario();
        return new AdminSugerenciaDTOResponse(
                entity.getId(),
                entity.getTexto(),
                entity.getFecha(),
                usuario.getNombre(),
                usuario.getEmail(),
                nombreDelRol(usuario));
    }

    // Una persona tiene un solo rol; si por lo que sea no tiene ninguno, queda vacío
    private static String nombreDelRol(UsuarioEntity usuario) {
        if (usuario.getRoles() == null) {
            return "";
        }
        for (RoleEntity rol : usuario.getRoles()) {
            return rol.getName();
        }
        return "";
    }
}
