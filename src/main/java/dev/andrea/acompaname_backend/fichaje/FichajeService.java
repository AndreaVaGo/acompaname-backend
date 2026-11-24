package dev.andrea.acompaname_backend.fichaje;

import java.util.List;

import dev.andrea.acompaname_backend.fichaje.dtos.FichajeConfiguracionDTOResponse;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTOResponse;

public interface FichajeService {

    // La persona cuidadora ficha que empieza el servicio
    FichajeDTOResponse empezar(Long solicitudId, FichajeDTORequest ubicacion);

    // La persona cuidadora ficha que termina el servicio (antes tiene que haberlo empezado)
    FichajeDTOResponse terminar(Long solicitudId, FichajeDTORequest ubicacion);

    // Los fichajes de una solicitud: solo los ven su familia y su persona cuidadora
    List<FichajeDTOResponse> listar(Long solicitudId);

    // Si se comprueba el lugar con la ubicación (y por tanto el navegador tiene que pedirla)
    FichajeConfiguracionDTOResponse configuracion();
}
