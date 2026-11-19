package dev.andrea.acompaname_backend.geolocation;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Versión para cuando no se quiere salir a internet: nunca encuentra ningún lugar.
// Está activa mientras geocoding.enabled no sea "true".
@Service
@ConditionalOnProperty(name = "geocoding.enabled", havingValue = "false", matchIfMissing = true)
public class GeocodificadorDesactivado implements Geocodificador {

    @Override
    public Optional<Coordenadas> buscar(String lugar) {
        return Optional.empty();
    }
}
