package dev.andrea.acompaname_backend.servicerequest.mappers;

import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SolicitudMapper {

    // Pasa la lista de necesidades a un texto con comas para guardarlo en la base de datos
    private static String necesidadesATexto(List<String> necesidades) {
        if (necesidades == null || necesidades.isEmpty()) {
            return null;
        }
        return String.join(",", necesidades);
    }

    // Pasa el texto de la base de datos a una lista (si no hay nada, lista vacía)
    private static List<String> necesidadesALista(String necesidades) {
        List<String> lista = new ArrayList<>();
        if (necesidades == null || necesidades.isBlank()) {
            return lista;
        }
        for (String necesidad : necesidades.split(",")) {
            lista.add(necesidad);
        }
        return lista;
    }

    public static SolicitudEntity toEntity(SolicitudDTORequest dto, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTipoCuidado(dto.tipoCuidado());
        solicitud.setNombrePaciente(dto.nombrePaciente());
        solicitud.setNotas(dto.notas());
        solicitud.setEdadPaciente(dto.edadPaciente());
        solicitud.setFechaCuidado(dto.fechaCuidado());
        solicitud.setFechaFin(dto.fechaFin());
        solicitud.setNecesidades(necesidadesATexto(dto.necesidades()));
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        return solicitud;
    }

    public static SolicitudDTOResponse toDTO(SolicitudEntity entity) {
        return new SolicitudDTOResponse(entity.getId(), entity.getTipoCuidado(), entity.getNombrePaciente(),
                entity.getNotas(), entity.getEdadPaciente(), entity.getFechaCuidado(), entity.getFechaFin(),
                entity.getEstado(), entity.getFamilia().getId(), entity.getCuidador().getId(),
                entity.getFamilia().getNombre(), entity.getCuidador().getUsuario().getNombre(),
                necesidadesALista(entity.getNecesidades()));
    }

}