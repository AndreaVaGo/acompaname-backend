package dev.andrea.acompaname_backend.review;

import java.util.List;

import dev.andrea.acompaname_backend.generics.InterfaceGenericService;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTOResponse;
import dev.andrea.acompaname_backend.review.dtos.ValoracionPublicaDTOResponse;

public interface ValoracionService extends InterfaceGenericService<ValoracionEntity, ValoracionDTORequest, ValoracionDTOResponse> {

    // Valoraciones que cualquier visitante puede ver de un cuidador
    List<ValoracionPublicaDTOResponse> getPublicasPorCuidador(Long cuidadorId);

}
