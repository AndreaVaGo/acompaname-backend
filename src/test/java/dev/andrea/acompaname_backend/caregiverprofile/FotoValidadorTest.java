package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionFotoInvalida;

// Tests del validador de fotos, sin Spring: se crea con el máximo que queramos (aquí 1000 bytes)
public class FotoValidadorTest {

    private static final String CABECERA_JPEG = "data:image/jpeg;base64,";
    private static final String CABECERA_PNG = "data:image/png;base64,";
    private static final String CABECERA_WEBP = "data:image/webp;base64,";

    private final FotoValidador validador = new FotoValidador(1000);

    // Monta la foto como llega del front: la cabecera y los bytes en Base64
    private String comoTexto(String cabecera, byte[] bytes) {
        return cabecera + Base64.getEncoder().encodeToString(bytes);
    }

    // Una imagen JPG falsa del tamaño que se pida: solo tiene bien los 3 primeros bytes
    private byte[] jpeg(int tamano) {
        byte[] bytes = new byte[tamano];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    // Una imagen PNG falsa: solo tiene bien los 8 primeros bytes
    private byte[] png() {
        return new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0 };
    }

    // Una imagen WebP falsa: "RIFF", 4 bytes cualquiera y "WEBP"
    private byte[] webp() {
        return "RIFFabcdWEBP".getBytes(StandardCharsets.US_ASCII);
    }

    // Comprueba que la foto se rechaza y que el mensaje de error dice lo esperado
    private void comprobarQueSeRechaza(String foto, String textoDelMensaje) {
        PerfilCuidadorExceptionFotoInvalida error = assertThrows(PerfilCuidadorExceptionFotoInvalida.class,
                () -> validador.comprobar(foto));

        assertThat(error.getMessage(), containsString(textoDelMensaje));
    }

    // ---------- Fotos que se aceptan ----------

    @Test
    void testUnaFotoJpgValidaPasa() {
        assertDoesNotThrow(() -> validador.comprobar(comoTexto(CABECERA_JPEG, jpeg(500))));
    }

    @Test
    void testUnaFotoPngValidaPasa() {
        assertDoesNotThrow(() -> validador.comprobar(comoTexto(CABECERA_PNG, png())));
    }

    @Test
    void testUnaFotoWebpValidaPasa() {
        assertDoesNotThrow(() -> validador.comprobar(comoTexto(CABECERA_WEBP, webp())));
    }

    @Test
    void testSinFotoPasa() {
        assertDoesNotThrow(() -> validador.comprobar(null));
        assertDoesNotThrow(() -> validador.comprobar(""));
        assertDoesNotThrow(() -> validador.comprobar("   "));
    }

    @Test
    void testUnaFotoJustoEnElLimiteDeTamanoPasa() {
        assertDoesNotThrow(() -> validador.comprobar(comoTexto(CABECERA_JPEG, jpeg(1000))));
    }

    // ---------- Formatos que no se permiten ----------

    @Test
    void testUnSvgSeRechaza() {
        String svg = comoTexto("data:image/svg+xml;base64,", "<svg></svg>".getBytes(StandardCharsets.US_ASCII));

        comprobarQueSeRechaza(svg, "JPG, PNG o WebP");
    }

    @Test
    void testUnGifSeRechaza() {
        String gif = comoTexto("data:image/gif;base64,", "GIF89a".getBytes(StandardCharsets.US_ASCII));

        comprobarQueSeRechaza(gif, "JPG, PNG o WebP");
    }

    @Test
    void testUnaDireccionWebSeRechaza() {
        comprobarQueSeRechaza("https://ejemplo.com/foto.jpg", "JPG, PNG o WebP");
    }

    @Test
    void testUnTextoCualquieraSeRechaza() {
        comprobarQueSeRechaza("esto no es una foto", "JPG, PNG o WebP");
    }

    // ---------- Texto que no es Base64 ----------

    @Test
    void testUnTextoQueNoEsBase64SeRechaza() {
        comprobarQueSeRechaza(CABECERA_JPEG + "esto no es base64!!", "no es una imagen válida");
    }

    // ---------- Tamaño ----------

    @Test
    void testUnaFotoMuyGrandeSeRechazaSinDecodificarla() {
        // 2000 bytes en Base64 ocupan más del doble del máximo (1000)
        comprobarQueSeRechaza(comoTexto(CABECERA_JPEG, jpeg(2000)), "demasiado grande");
    }

    @Test
    void testUnaFotoQueSuperaElLimiteSeRechazaDespuesDeDecodificarla() {
        // 1500 bytes: el texto no llega al doble del máximo, pero la imagen sí pasa de 1000
        comprobarQueSeRechaza(comoTexto(CABECERA_JPEG, jpeg(1500)), "demasiado grande");
    }

    @Test
    void testElMensajeDeTamanoDiceElMaximo() {
        comprobarQueSeRechaza(comoTexto(CABECERA_JPEG, jpeg(1500)), "El máximo es 1 KB");
    }

    // ---------- El contenido real tiene que coincidir con el formato ----------

    @Test
    void testUnPngQueDiceSerJpgSeRechaza() {
        comprobarQueSeRechaza(comoTexto(CABECERA_JPEG, png()), "no coincide con su formato");
    }

    @Test
    void testUnTextoQueDiceSerPngSeRechaza() {
        byte[] texto = "<script>alert(1)</script>".getBytes(StandardCharsets.US_ASCII);

        comprobarQueSeRechaza(comoTexto(CABECERA_PNG, texto), "no coincide con su formato");
    }

    @Test
    void testUnJpgQueDiceSerWebpSeRechaza() {
        comprobarQueSeRechaza(comoTexto(CABECERA_WEBP, jpeg(100)), "no coincide con su formato");
    }

    @Test
    void testUnWebpSinLaMarcaWebpSeRechaza() {
        // Empieza por "RIFF" pero en la posición 8 pone "WAVE" (un archivo de audio)
        byte[] audio = "RIFFabcdWAVE".getBytes(StandardCharsets.US_ASCII);

        comprobarQueSeRechaza(comoTexto(CABECERA_WEBP, audio), "no coincide con su formato");
    }

    @Test
    void testUnaImagenDemasiadoCortaSeRechaza() {
        // Solo 2 bytes: ni siquiera caben los 3 que identifican a un JPG
        byte[] corta = { (byte) 0xFF, (byte) 0xD8 };

        comprobarQueSeRechaza(comoTexto(CABECERA_JPEG, corta), "no coincide con su formato");
    }
}