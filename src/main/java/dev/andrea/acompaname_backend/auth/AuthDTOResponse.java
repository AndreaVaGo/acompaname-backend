package dev.andrea.acompaname_backend.auth;

// Respuesta del login: datos de la persona y el token que el front enviará en cada petición
public record AuthDTOResponse(Long id, String message, String username, String roles, String token) {

}
