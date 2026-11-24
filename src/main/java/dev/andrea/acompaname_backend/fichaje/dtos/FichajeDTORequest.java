package dev.andrea.acompaname_backend.fichaje.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

// La ubicación que manda el navegador al fichar. Es opcional: sin ella el fichaje se guarda como "sin ubicación".
// Solo sirve para calcular la distancia en ese momento: el servidor NO la guarda
public record FichajeDTORequest(
        @DecimalMin(value = "-90.0", message = "La latitud tiene que estar entre -90 y 90") @DecimalMax(value = "90.0", message = "La latitud tiene que estar entre -90 y 90") Double latitud,
        @DecimalMin(value = "-180.0", message = "La longitud tiene que estar entre -180 y 180") @DecimalMax(value = "180.0", message = "La longitud tiene que estar entre -180 y 180") Double longitud) {

    @AssertTrue(message = "La latitud y la longitud se mandan juntas")
    public boolean isUbicacionCompleta() {
        return (latitud == null) == (longitud == null);
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
}
