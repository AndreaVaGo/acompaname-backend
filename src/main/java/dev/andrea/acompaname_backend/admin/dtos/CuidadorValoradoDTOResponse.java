package dev.andrea.acompaname_backend.admin.dtos;

// Un cuidador de la lista de mejor valorados del panel de administración
public record CuidadorValoradoDTOResponse(
        Long id,
        String nombre,
        double notaMedia,
        long serviciosCompletados) {

}