package dev.andrea.acompaname_backend.geolocation.dtos;

// A cuántos kilómetros (en línea recta, de ciudad a ciudad) está una persona cuidadora del lugar buscado
public record DistanciaCuidadorDTOResponse(Long cuidadorId, double km) {
}
