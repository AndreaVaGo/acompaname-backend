package dev.andrea.acompaname_backend.generics;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

// Una página de una lista larga: el contenido de esa página y lo necesario para pedir las siguientes.
// pagina empieza en 0. Se usa en todas las listas que se paginan (ACOM-234)
public record PaginaDTOResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    // Convierte una página de entidades en una página de DTOs
    public static <E, T> PaginaDTOResponse<T> de(Page<E> pagina, Function<E, T> convertir) {
        List<T> contenido = pagina.getContent().stream().map(convertir).toList();
        return new PaginaDTOResponse<>(contenido, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
