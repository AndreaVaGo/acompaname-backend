package dev.andrea.acompaname_backend.security;

// Se lanza cuando una misma dirección IP hace demasiadas peticiones seguidas
public class DemasiadasPeticionesException extends RuntimeException {

    public DemasiadasPeticionesException(String mensaje) {
        super(mensaje);
    }
}