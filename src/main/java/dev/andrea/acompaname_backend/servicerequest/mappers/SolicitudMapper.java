package dev.andrea.acompaname_backend.servicerequest.mappers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.PersonaCuidada;
import dev.andrea.acompaname_backend.servicerequest.PersonasDeSolicitud;
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
        // La primera persona de la lista también se guarda en nombrePaciente y edadPaciente
        List<PersonaCuidada> personas = PersonasDeSolicitud.desdePeticion(dto);
        solicitud.setPersonas(personas);
        solicitud.setNombrePaciente(personas.get(0).getNombre());
        solicitud.setEdadPaciente(personas.get(0).getEdad());
        solicitud.setNotas(dto.notas());
        solicitud.setFechaCuidado(dto.fechaCuidado());
        solicitud.setFechaFin(dto.fechaFin());
        solicitud.setNecesidades(necesidadesATexto(dto.necesidades()));
        solicitud.setDireccion(dto.direccion());
        // El grupo de la solicitud es el de la primera persona (o el que llegue suelto si ella no tiene)
        if (personas.get(0).getGrupoEdad() != null) {
            solicitud.setGrupoEdad(personas.get(0).getGrupoEdad());
        } else {
            solicitud.setGrupoEdad(dto.grupoEdad());
        }
        solicitud.setHoraInicio(dto.horaInicio());
        solicitud.setHoraFin(dto.horaFin());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now());
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        return solicitud;
    }

    public static SolicitudDTOResponse toDTO(SolicitudEntity entity) {
        return new SolicitudDTOResponse(entity.getId(), entity.getTipoCuidado(), entity.getNombrePaciente(),
                entity.getNotas(), entity.getEdadPaciente(), entity.getFechaCuidado(), entity.getFechaFin(),
                entity.getEstado(), entity.getFamilia().getId(), entity.getCuidador().getId(),
                entity.getFamilia().getNombre(), entity.getCuidador().getUsuario().getNombre(),
                necesidadesALista(entity.getNecesidades()), entity.getDireccion(), entity.getGrupoEdad(),
                entity.getHoraInicio(), entity.getHoraFin(), PersonasDeSolicitud.aDTO(entity));
    }

}