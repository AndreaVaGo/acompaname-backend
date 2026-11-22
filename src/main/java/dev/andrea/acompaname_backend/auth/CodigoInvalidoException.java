package dev.andrea.acompaname_backend.auth;

// El código del segundo paso no vale: no existe, ya se usó, ha caducado, se han gastado los intentos
// o es incorrecto. Siempre el mismo mensaje, para no dar pistas de cuál de los casos es.
public class CodigoInvalidoException extends RuntimeException {

    public CodigoInvalidoException() {
        super("El código no es válido o ha caducado. Vuelve a iniciar sesión.");
    }
}
