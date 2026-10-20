package dev.andrea.acompaname_backend.admin.dtos;

import java.time.LocalDate;

// Una valoración tal como la ve el administrador en el panel (con los nombres de la familia y del cuidador)
public record AdminValoracionDTOResponse(
        Long id,
        Integer puntuacion,
        String comentario,
        LocalDate fecha,
        String nombreFamilia,
        String nombreCuidador) {

}