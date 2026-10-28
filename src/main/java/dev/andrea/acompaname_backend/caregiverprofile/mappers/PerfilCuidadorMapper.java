package dev.andrea.acompaname_backend.caregiverprofile.mappers;

import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.globals.ListaDeTexto;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PerfilCuidadorMapper {

    // Si el cuidador no ha elegido tipo de cuidado, se considera que ofrece los dos
    private static String tipoCuidadoOPorDefecto(String tipoCuidado) {
        return tipoCuidado == null ? "ambos" : tipoCuidado;
    }

    // Si el sexo viene vacío, se guarda null (no lo ha indicado)
    private static String sexoONull(String sexo) {
        if (sexo == null || sexo.isBlank()) {
            return null;
        }
        return sexo;
    }

    // Pasa la lista de servicios a un texto con comas para guardarlo en la base de datos
    private static String serviciosATexto(List<String> servicios) {
        if (servicios == null || servicios.isEmpty()) {
            return null;
        }
        return String.join(",", servicios);
    }

    // Pasa el texto de la base de datos a una lista (si no hay nada, lista vacía)
    private static List<String> serviciosALista(String servicios) {
        List<String> lista = new ArrayList<>();
        if (servicios == null || servicios.isBlank()) {
            return lista;
        }
        for (String servicio : servicios.split(",")) {
            lista.add(servicio);
        }
        return lista;
    }

    // Pasa los grupos de edad a un texto con comas ("BEBES,NINOS") sin repetir ninguno.
    // Es público porque el servicio también lo necesita al actualizar el perfil
    public static String gruposEdadATexto(List<GrupoEdad> grupos) {
        if (grupos == null) {
            return null;
        }
        List<String> nombres = grupos.stream().map(GrupoEdad::name).distinct().toList();
        return ListaDeTexto.aTexto(nombres);
    }

    // Pasa el texto de la base de datos a una lista de grupos (si no hay nada, lista vacía)
    private static List<GrupoEdad> gruposEdadALista(String texto) {
        return ListaDeTexto.aLista(texto).stream().map(GrupoEdad::valueOf).toList();
    }

    public static PerfilCuidadorEntity toEntity(PerfilCuidadorDTORequest dto, UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setEspecialidad(dto.especialidad());
        perfil.setAnosExperiencia(dto.anosExperiencia());
        perfil.setTarifaHora(dto.tarifaHora());
        perfil.setBio(dto.bio());
        perfil.setTieneVehiculo(dto.tieneVehiculo());
        perfil.setDisponibleAhora(dto.disponibleAhora());
        perfil.setFoto(dto.foto());
        perfil.setTipoCuidado(tipoCuidadoOPorDefecto(dto.tipoCuidado()));
        perfil.setServicios(serviciosATexto(dto.servicios()));
        perfil.setSexo(sexoONull(dto.sexo()));
        perfil.setCiudad(dto.ciudad());
        perfil.setGruposEdad(gruposEdadATexto(dto.gruposEdad()));
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
                numValoraciones,
                tipoCuidadoOPorDefecto(entity.getTipoCuidado()),
                serviciosALista(entity.getServicios()),
                entity.getSexo(),
                entity.getCiudad(),
                gruposEdadALista(entity.getGruposEdad()));
        return dtoResponse;

    }

}