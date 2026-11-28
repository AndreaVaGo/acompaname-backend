package dev.andrea.acompaname_backend.schedule;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Cifra la dirección secreta del calendario antes de guardarla en la base de datos (ACOM-261).
// Quien tenga esa dirección puede leer el calendario, así que si alguien consigue una copia de la base de datos
// no debe poder usarla. La clave NO está en la base de datos: viene de la variable CALENDARIO_CLAVE.
//
// Se usa AES-GCM (cifra y comprueba que nadie ha tocado el valor). Cada valor lleva su propio "nonce" aleatorio.
// El valor guardado empieza por "enc:v1:". Un valor sin ese prefijo es una dirección antigua, guardada en claro
// antes de este cambio: se lee tal cual y se vuelve a guardar cifrada en la siguiente lectura.
@Component
public class CifradorDeUrl {

    // Tiene que ser igual al valor por defecto de calendario.clave en application.properties (hay un test que lo comprueba)
    static final String CLAVE_DE_DESARROLLO = "clave-calendario-solo-para-desarrollo-local";

    static final String PREFIJO = "enc:v1:";

    private static final List<String> ORIGENES_LOCALES = List.of("localhost", "127.0.0.1", "[::1]");
    private static final int BYTES_NONCE = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorDeUrl(@Value("${calendario.clave}") String passphrase,
            @Value("${cors.allowed-origins}") String[] origenesPermitidos) {
        comprobar(passphrase, origenesPermitidos);
        try {
            // Se obtiene una clave de 256 bits a partir de la frase
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(passphrase.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(hash, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo preparar el cifrado del calendario", e);
        }
    }

    // Igual que GuardiaDeClaveJwt: con la clave de desarrollo (pública) la app no arranca en un servidor real
    static void comprobar(String passphrase, String[] origenesPermitidos) {
        if (passphrase == null || passphrase.isBlank()) {
            throw new IllegalStateException("calendario.clave está vacía. Define CALENDARIO_CLAVE");
        }
        if (!CLAVE_DE_DESARROLLO.equals(passphrase)) {
            return;
        }
        for (String origen : origenesPermitidos) {
            if (!esLocal(origen)) {
                throw new IllegalStateException("calendario.clave es la clave de desarrollo, pero cors.allowed-origins incluye "
                        + origen + ", que no es local. En un servidor hay que definir CALENDARIO_CLAVE con una clave propia "
                        + "y arrancar con SPRING_PROFILES_ACTIVE=prod");
            }
        }
    }

    private static boolean esLocal(String origen) {
        try {
            String host = URI.create(origen.trim()).getHost();
            return host != null && ORIGENES_LOCALES.contains(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String cifrar(String texto) {
        try {
            byte[] nonce = new byte[BYTES_NONCE];
            aleatorio.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, nonce));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            byte[] todo = new byte[nonce.length + cifrado.length];
            System.arraycopy(nonce, 0, todo, 0, nonce.length);
            System.arraycopy(cifrado, 0, todo, nonce.length, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar la dirección del calendario", e);
        }
    }

    // Si el valor no lleva el prefijo es una dirección antigua en claro y se devuelve tal cual
    public String descifrar(String guardado) {
        if (!estaCifrado(guardado)) {
            return guardado;
        }
        try {
            byte[] todo = Base64.getDecoder().decode(guardado.substring(PREFIJO.length()));
            if (todo.length <= BYTES_NONCE) {
                throw new IllegalStateException("La dirección guardada del calendario no es válida");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, todo, 0, BYTES_NONCE));
            byte[] claro = cipher.doFinal(todo, BYTES_NONCE, todo.length - BYTES_NONCE);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Clave cambiada o valor manipulado. No se da más detalle (ni el valor) en el mensaje
            throw new IllegalStateException("No se pudo leer la dirección guardada del calendario. Vuelve a vincularlo", e);
        }
    }

    public boolean estaCifrado(String guardado) {
        return guardado != null && guardado.startsWith(PREFIJO);
    }
}
