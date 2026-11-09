package dev.andrea.acompaname_backend.caregiverprofile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionFotoInvalida;

// Comprueba la foto de perfil que llega al back. El front ya la reduce, pero el back no
// puede fiarse de eso: alguien podría enviar la petición sin pasar por el front.
// La foto llega como texto con este formato: "data:image/jpeg;base64,/9j/4AAQ..."
// El tamaño máximo sale de application.properties (foto.max-bytes)
@Component
public class FotoValidador {

    private static final String CABECERA_JPEG = "data:image/jpeg;base64,";
    private static final String CABECERA_PNG = "data:image/png;base64,";
    private static final String CABECERA_WEBP = "data:image/webp;base64,";

    private final int maximoDeBytes;

    public FotoValidador(@Value("${foto.max-bytes}") int maximoDeBytes) {
        this.maximoDeBytes = maximoDeBytes;
    }

    // Si la foto no vale, lanza el error con el motivo. Sin foto (null o vacía) no hay nada que comprobar
    public void comprobar(String foto) {
        if (foto == null || foto.isBlank()) {
            return;
        }

        // 1. Solo se permiten JPG, PNG y WebP
        String cabecera;
        if (foto.startsWith(CABECERA_JPEG)) {
            cabecera = CABECERA_JPEG;
        } else if (foto.startsWith(CABECERA_PNG)) {
            cabecera = CABECERA_PNG;
        } else if (foto.startsWith(CABECERA_WEBP)) {
            cabecera = CABECERA_WEBP;
        } else {
            throw new PerfilCuidadorExceptionFotoInvalida("La foto debe ser JPG, PNG o WebP.");
        }
        String datos = foto.substring(cabecera.length());

        // 2. Tamaño aproximado antes de decodificar (cada 4 caracteres del texto son 3 bytes),
        // para no decodificar un texto enorme
        if (datos.length() / 4 * 3 > maximoDeBytes) {
            throw fotoDemasiadoGrande();
        }

        // 3. El texto tiene que ser Base64 de verdad
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(datos);
        } catch (IllegalArgumentException error) {
            throw new PerfilCuidadorExceptionFotoInvalida("La foto no es una imagen válida.");
        }

        // 4. Tamaño exacto, ya decodificada
        if (bytes.length > maximoDeBytes) {
            throw fotoDemasiadoGrande();
        }

        // 5. Se mira el contenido real: los primeros bytes de cada formato son siempre los mismos
        // y tienen que coincidir con el formato que dice la cabecera
        boolean contenidoCorrecto;
        if (cabecera.equals(CABECERA_JPEG)) {
            contenidoCorrecto = esJpeg(bytes);
        } else if (cabecera.equals(CABECERA_PNG)) {
            contenidoCorrecto = esPng(bytes);
        } else {
            contenidoCorrecto = esWebp(bytes);
        }
        if (!contenidoCorrecto) {
            throw new PerfilCuidadorExceptionFotoInvalida("El contenido de la foto no coincide con su formato.");
        }
    }

    private PerfilCuidadorExceptionFotoInvalida fotoDemasiadoGrande() {
        return new PerfilCuidadorExceptionFotoInvalida(
                "La foto es demasiado grande. El máximo es " + (maximoDeBytes / 1000) + " KB.");
    }

    // JPG empieza siempre por los bytes FF D8 FF
    private boolean esJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    // PNG empieza siempre por estos 8 bytes (137, 80, 78, 71, 13, 10, 26, 10)
    private boolean esPng(byte[] bytes) {
        int[] firma = { 137, 80, 78, 71, 13, 10, 26, 10 };
        if (bytes.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((bytes[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    // WebP empieza por el texto "RIFF", y en la posición 8 lleva el texto "WEBP"
    private boolean esWebp(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        String inicio = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        String formato = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
        return inicio.equals("RIFF") && formato.equals("WEBP");
    }
}