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
        perfil.setCiudad(dto.ciudad());
        perfil.setUsuario(usuario);
        return perfil;
    }

    public static PerfilCuidadorDTOResponse toDTO(PerfilCuidadorEntity entity) {
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
        entity.getCiudad());
        return dtoResponse;

    }

}
