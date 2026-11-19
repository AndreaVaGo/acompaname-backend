package dev.andrea.acompaname_backend.schedule.dtos;

// Resumen de una importación: cuántos bloques se han creado, cuántos ya existían
// y cuántos eventos se han dejado fuera (repetidos, cancelados, ya pasados o que no se entienden)
public record ImportacionCalendarioDTOResponse(int importados, int duplicados, int omitidos) {
}
