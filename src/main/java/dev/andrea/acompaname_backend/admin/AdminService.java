package dev.andrea.acompaname_backend.admin;

import java.util.List;

import dev.andrea.acompaname_backend.admin.dtos.AdminSolicitudDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminUsuarioDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AdminValoracionDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;

public interface AdminService {

    EstadisticasDTOResponse estadisticas();

    AvisosDTOResponse avisos();

    // Los tres cuidadores con mejor nota media
    List<CuidadorValoradoDTOResponse> mejoresCuidadores();

    // Las cinco valoraciones más recientes
    List<AdminValoracionDTOResponse> ultimasValoraciones();

    // Borra una valoración inapropiada (404 si no existe)
    void borrarValoracion(Long id);

    // Todos los usuarios, sin contraseñas
    List<AdminUsuarioDTOResponse> usuarios();

    // Bloquea (activo = false) o desbloquea (activo = true) a un usuario.
    // 404 si no existe y 403 si el administrador intenta bloquearse a sí mismo.
    AdminUsuarioDTOResponse cambiarActivo(Long id, boolean activo);

    // Todas las solicitudes, o solo las de un estado si se indica (null = todas)
    List<AdminSolicitudDTOResponse> solicitudes(EstadoSolicitud estado);
}