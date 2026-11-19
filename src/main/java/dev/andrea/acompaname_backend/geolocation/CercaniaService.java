package dev.andrea.acompaname_backend.geolocation;

import dev.andrea.acompaname_backend.geolocation.dtos.CercaniaDTOResponse;

public interface CercaniaService {

    // Distancia desde el lugar escrito hasta la ciudad de cada persona cuidadora
    CercaniaDTOResponse buscarCerca(String lugar);
}
