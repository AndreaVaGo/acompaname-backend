package dev.andrea.acompaname_backend.caregiverprofile;

import java.util.Base64;

// La foto de un perfil lista para enviarla como imagen: su tipo (image/jpeg...) y sus bytes.
// En la base de datos la foto está guardada como texto: "data:image/jpeg;base64,/9j/4AAQ..."
public record FotoDescargable(String tipo, byte[] bytes) {

    private static final String PREFIJO = "data:image/";

    // Solo las fotos guardadas como texto con ese formato se pueden enviar como imagen
    public static boolean esDescargable(String foto) {
        return foto != null && foto.startsWith(PREFIJO) && foto.contains(";base64,");
    }

    public static FotoDescargable de(String foto) {
        int finDeCabecera = foto.indexOf(";base64,");
        String tipo = foto.substring("data:".length(), finDeCabecera);
        byte[] bytes = Base64.getDecoder().decode(foto.substring(finDeCabecera + ";base64,".length()));
        return new FotoDescargable(tipo, bytes);
    }
}
