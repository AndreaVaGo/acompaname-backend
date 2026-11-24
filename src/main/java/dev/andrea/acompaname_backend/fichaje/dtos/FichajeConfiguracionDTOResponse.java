package dev.andrea.acompaname_backend.fichaje.dtos;

// Lo que necesita saber el navegador antes de fichar: si tiene que pedir la ubicación a la persona cuidadora.
// Si está apagado, no se pide (no se pide un dato que luego no se va a usar)
public record FichajeConfiguracionDTOResponse(boolean verificarLugar) {
}
