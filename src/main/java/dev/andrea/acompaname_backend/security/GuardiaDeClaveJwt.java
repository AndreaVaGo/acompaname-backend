package dev.andrea.acompaname_backend.security;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Impide que la aplicación arranque en un servidor real con la clave JWT de desarrollo.
//
// La clave de desarrollo es pública (está en application.properties), así que cualquiera podría
// firmar tokens falsos, incluso de administrador. Pasaría si en el servidor se olvida definir
// SPRING_PROFILES_ACTIVE=prod (el perfil prod no tiene clave por defecto y obliga a definir JWT_SECRET).
//
// La señal de que es un servidor real: algún origen permitido de CORS no es local. Sin ese origen
// el front no puede llamar a la API, así que en un servidor siempre está. En local nunca.
@Component
public class GuardiaDeClaveJwt {

    // Tiene que ser igual al valor por defecto de jwt.secret en application.properties (hay un test que lo comprueba)
    static final String CLAVE_DE_DESARROLLO = "clave-solo-para-desarrollo-local-cambiala-en-produccion";

    private static final List<String> ORIGENES_LOCALES = List.of("localhost", "127.0.0.1", "[::1]");

    public GuardiaDeClaveJwt(@Value("${jwt.secret}") String secreto,
            @Value("${cors.allowed-origins}") String[] origenesPermitidos) {
        comprobar(secreto, origenesPermitidos);
    }

    static void comprobar(String secreto, String[] origenesPermitidos) {
        if (!CLAVE_DE_DESARROLLO.equals(secreto)) {
            return;
        }
        for (String origen : origenesPermitidos) {
            if (!esLocal(origen)) {
                throw new IllegalStateException("jwt.secret es la clave de desarrollo, pero cors.allowed-origins incluye "
                        + origen + ", que no es local. En un servidor hay que definir JWT_SECRET con una clave propia "
                        + "y arrancar con SPRING_PROFILES_ACTIVE=prod");
            }
        }
    }

    private static boolean esLocal(String origen) {
        try {
            String host = URI.create(origen.trim()).getHost();
            return host != null && ORIGENES_LOCALES.contains(host);
        } catch (IllegalArgumentException e) {
            // Un origen que no se entiende no se da por local
            return false;
        }
    }
}
