package dev.andrea.acompaname_backend.admin;

import java.util.List;

import dev.andrea.acompaname_backend.admin.dtos.AdminValoracionDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.AvisosDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.CuidadorValoradoDTOResponse;
import dev.andrea.acompaname_backend.admin.dtos.EstadisticasDTOResponse;

public interface AdminService {

    EstadisticasDTOResponse estadisticas();

    AvisosDTOResponse avisos();

    // Los tres cuidadores con mejor nota media
    List<CuidadorValoradoDTOResponse> mejoresCuidadores();

    // Las cinco valoraciones más recientes
    List<AdminValoracionDTOResponse> ultimasValoraciones();

    // Borra una valoración inapropiada (404 si no existe)
    void borrarValoracion(Long id);
}