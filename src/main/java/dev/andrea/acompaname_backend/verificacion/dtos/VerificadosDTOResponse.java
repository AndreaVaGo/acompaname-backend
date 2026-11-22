package dev.andrea.acompaname_backend.verificacion.dtos;

// Lo que ve cualquier familia (sin iniciar sesión): solo dos "sí/no" por cuidador.
// Nunca se enseña la referencia ni quién la revisó
public record VerificadosDTOResponse(Long cuidadorId, boolean titulacionVerificada, boolean antecedentesVerificados) {

}
