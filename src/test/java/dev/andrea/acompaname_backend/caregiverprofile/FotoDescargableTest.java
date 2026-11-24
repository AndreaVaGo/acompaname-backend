package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Base64;

import org.junit.jupiter.api.Test;

// La foto guardada como texto se convierte en imagen (tipo y bytes) para enviarla aparte
public class FotoDescargableTest {

    private static String textoDeFoto(String tipo, byte[] bytes) {
        return "data:image/" + tipo + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void testSacaElTipoYLosBytesDeUnaFotoJpeg() {
        byte[] bytes = { 1, 2, 3, 4 };

        FotoDescargable foto = FotoDescargable.de(textoDeFoto("jpeg", bytes));

        assertThat(foto.tipo(), is(equalTo("image/jpeg")));
        assertThat(foto.bytes(), is(equalTo(bytes)));
    }

    @Test
    void testFuncionaConPngYWebp() {
        assertThat(FotoDescargable.de(textoDeFoto("png", new byte[] { 9 })).tipo(), is(equalTo("image/png")));
        assertThat(FotoDescargable.de(textoDeFoto("webp", new byte[] { 9 })).tipo(), is(equalTo("image/webp")));
    }

    @Test
    void testUnaFotoGuardadaComoTextoDeImagenSePuedeDescargar() {
        assertThat(FotoDescargable.esDescargable(textoDeFoto("png", new byte[] { 1 })), is(true));
    }

    @Test
    void testUnaDireccionExternaOUnTextoVacioNoSePuedenDescargar() {
        assertThat(FotoDescargable.esDescargable("https://ejemplo.com/foto.jpg"), is(false));
        assertThat(FotoDescargable.esDescargable(""), is(false));
        assertThat(FotoDescargable.esDescargable(null), is(false));
    }
}
