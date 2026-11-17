package dev.andrea.acompaname_backend.suggestion;

import java.util.List;

import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;

public interface SugerenciaService {

    // Guarda la sugerencia de la persona que tiene la sesión iniciada
    SugerenciaDTOResponse crear(SugerenciaDTORequest dto);

    // Todas las sugerencias, las más recientes primero (solo para el administrador)
    List<AdminSugerenciaDTOResponse> listar();
}
