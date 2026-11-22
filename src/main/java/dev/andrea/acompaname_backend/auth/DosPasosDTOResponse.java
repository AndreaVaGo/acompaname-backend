package dev.andrea.acompaname_backend.auth;

// Respuesta del login cuando la persona tiene el segundo paso activado: todavía no hay token.
// El front tiene que pedir el código que llega por correo y enviarlo junto con este desafío.
public record DosPasosDTOResponse(boolean requiereCodigo, String desafio) {

}
