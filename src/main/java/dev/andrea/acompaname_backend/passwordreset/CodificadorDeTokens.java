package dev.andrea.acompaname_backend.passwordreset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Crea los tokens del enlace de recuperación y calcula su resumen (hash)
public final class CodificadorDeTokens {

    // 32 bytes al azar = 256 bits: imposible de adivinar
    private static final int BYTES_DEL_TOKEN = 32;

    // SecureRandom es el generador de números al azar preparado para seguridad (Random no vale)
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodificadorDeTokens() {
    }

    // Token nuevo, con letras, números, "-" y "_" para poder ponerlo en un enlace sin problemas
    public static String crear() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Resumen SHA-256 del token en hexadecimal (64 caracteres). El mismo token da siempre el mismo resumen,
    // pero desde el resumen no se puede volver al token
    public static String hash(String token) {
        try {
            MessageDigest resumidor = MessageDigest.getInstance("SHA-256");
            byte[] resumen = resumidor.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte unByte : resumen) {
                hexadecimal.append(String.format("%02x", unByte));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
