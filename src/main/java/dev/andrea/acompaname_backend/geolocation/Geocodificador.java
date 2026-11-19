package dev.andrea.acompaname_backend.geolocation;

import java.util.Optional;

// Convierte el nombre de un lugar (una ciudad, un código postal...) en un punto del mapa.
// Tiene dos versiones, como el servicio de correo:
// - GeocodificadorNominatim (geocoding.enabled=true): consulta OpenStreetMap, sin clave de API
// - GeocodificadorDesactivado (geocoding.enabled=false): no encuentra nada, para no salir a internet
public interface Geocodificador {

    // Devuelve vacío si no se encuentra el lugar o si el servicio falla
    Optional<Coordenadas> buscar(String lugar);
}
