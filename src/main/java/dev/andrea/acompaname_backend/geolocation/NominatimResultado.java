package dev.andrea.acompaname_backend.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Un resultado de Nominatim. Solo nos interesan la latitud y la longitud (llegan como texto)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResultado(String lat, String lon) {
}
