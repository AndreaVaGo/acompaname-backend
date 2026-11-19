package dev.andrea.acompaname_backend.geolocation.dtos;

import java.util.List;

// "lugarEncontrado" es false si no se ha podido situar el lugar escrito en el mapa.
// "distancias" va ordenada de la más cerca a la más lejos
public record CercaniaDTOResponse(boolean lugarEncontrado, List<DistanciaCuidadorDTOResponse> distancias) {
}
