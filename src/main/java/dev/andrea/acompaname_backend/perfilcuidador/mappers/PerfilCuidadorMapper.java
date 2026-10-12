package dev.andrea.acompaname_backend.perfilcuidador.mappers;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;

public class PerfilCuidadorMapper {

    public static PerfilCuidadorEntity toEntity(PerfilCuidadorDTORequest dto, UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setEspecialidad(dto.especialidad());
        perfil.setAnosExperiencia(dto.anosExperiencia());
        perfil.setTarifaHora(dto.tarifaHora());
        perfil.setBio(dto.bio());
        perfil.setTieneVehiculo(dto.tieneVehiculo());
        perfil.setDisponibleAhora(dto.disponibleAhora());
        perfil.setFoto(dto.foto());
        perfil.setUsuario(usuario);
        return perfil;
    }

    // Sin valoraciones: la media queda en null y el número en 0
    public static PerfilCuidadorDTOResponse toDTO(PerfilCuidadorEntity entity) {
        return toDTO(entity, null, 0L);
    }

    public static PerfilCuidadorDTOResponse toDTO(PerfilCuidadorEntity entity, Double valoracionMedia,
            Long numValoraciones) {
        PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(
                entity.getId(),
                entity.getEspecialidad(),
                entity.getAnosExperiencia(),
                entity.getTarifaHora(),
                entity.getBio(),
                entity.isTieneVehiculo(),
                entity.isDisponibleAhora(),
                entity.getUsuario().getId(),
                entity.getUsuario().getNombre(),
                entity.getFoto(),
                valoracionMedia,
                numValoraciones);
        return dtoResponse;

    }

}
