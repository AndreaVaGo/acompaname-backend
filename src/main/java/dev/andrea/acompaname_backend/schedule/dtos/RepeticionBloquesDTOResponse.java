package dev.andrea.acompaname_backend.schedule.dtos;

// Resumen de repetir un rato ocupado: cuántos bloques se han creado y cuántos ya existían
public record RepeticionBloquesDTOResponse(int creados, int duplicados) {
}
