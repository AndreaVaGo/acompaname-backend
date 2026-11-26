package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

// Las protecciones al descargar el calendario: solo https, solo servidores conocidos y con tamaño máximo.
// No se descarga nada de internet: solo se prueban las comprobaciones.
public class DescargadorDeCalendarioTest {

    private final DescargadorDeCalendario descargador = new DescargadorDeCalendario(
            "calendar.google.com, outlook.office365.com ,icloud.com");

    private void assertRechaza(String url) {
        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> descargador.normalizar(url));
    }

    @Test
    void testAceptaUnaDireccionDeGoogle() {
        String url = "https://calendar.google.com/calendar/ical/ana%40gmail.com/private-abc/basic.ics";
        assertThat(descargador.normalizar(url), is(equalTo(url)));
    }

    @Test
    void testConvierteWebcalEnHttps() {
        assertThat(descargador.normalizar("webcal://p12-caldav.icloud.com/published/2/abc"),
                is(equalTo("https://p12-caldav.icloud.com/published/2/abc")));
    }

    @Test
    void testQuitaLosEspaciosDeLosLados() {
        assertThat(descargador.normalizar("  https://outlook.office365.com/owa/calendar/x/reachcalendar.ics \n"),
                is(equalTo("https://outlook.office365.com/owa/calendar/x/reachcalendar.ics")));
    }

    @Test
    void testAceptaSubdominiosDeUnServidorPermitido() {
        assertThat(descargador.hostPermitido("p12-caldav.icloud.com"), is(true));
    }

    @Test
    void testRechazaUnServidorQueSoloSeParece() {
        assertThat(descargador.hostPermitido("evilicloud.com"), is(false));
        assertThat(descargador.hostPermitido("icloud.com.malo.es"), is(false));
    }

    @Test
    void testRechazaUnServidorNoPermitido() {
        assertRechaza("https://example.com/calendario.ics");
    }

    @Test
    void testRechazaHttpSinCifrar() {
        assertRechaza("http://calendar.google.com/calendar.ics");
    }

    @Test
    void testRechazaOtrosTiposDeDireccion() {
        assertRechaza("file:///etc/passwd");
        assertRechaza("ftp://calendar.google.com/x.ics");
    }

    @Test
    void testRechazaLaDireccionLocalYLasInternas() {
        assertRechaza("https://localhost/calendario.ics");
        assertRechaza("https://127.0.0.1/calendario.ics");
        assertRechaza("https://169.254.169.254/latest/meta-data");
    }

    @Test
    void testRechazaElTrucoDelUsuarioEnLaDireccion() {
        assertRechaza("https://calendar.google.com@evil.com/x.ics");
    }

    @Test
    void testRechazaOtroPuerto() {
        assertRechaza("https://calendar.google.com:8443/x.ics");
    }

    @Test
    void testRechazaVacioONulo() {
        assertRechaza(null);
        assertRechaza("   ");
    }

    @Test
    void testRechazaUnaDireccionDemasiadoLarga() {
        assertRechaza("https://calendar.google.com/" + "a".repeat(DescargadorDeCalendario.MAXIMO_URL));
    }

    @Test
    void testRechazaUnaDireccionMalEscrita() {
        assertRechaza("https://calendar.google.com/ con espacios");
    }

    @Test
    void testElMensajeDeServidorNoPermitidoDiceCualesSirven() {
        BloqueOcupadoExceptionFechaInvalida error = assertThrows(BloqueOcupadoExceptionFechaInvalida.class,
                () -> descargador.normalizar("https://example.com/x.ics"));
        assertThat(error.getMessage(), containsString("Google, Outlook o iCloud"));
    }

    @Test
    void testLeeUnTextoPequeno() throws IOException {
        byte[] texto = "BEGIN:VCALENDAR\r\nEND:VCALENDAR".getBytes(StandardCharsets.UTF_8);
        assertThat(DescargadorDeCalendario.leerLimitado(new ByteArrayInputStream(texto)),
                is(equalTo("BEGIN:VCALENDAR\r\nEND:VCALENDAR")));
    }

    @Test
    void testRechazaUnCalendarioDemasiadoGrande() {
        byte[] enorme = new byte[DescargadorDeCalendario.MAXIMO_BYTES + 1];
        assertThrows(BloqueOcupadoExceptionFechaInvalida.class,
                () -> DescargadorDeCalendario.leerLimitado(new ByteArrayInputStream(enorme)));
    }

    @Test
    void testAceptaUnCalendarioJustoEnElLimite() throws IOException {
        byte[] justo = new byte[DescargadorDeCalendario.MAXIMO_BYTES];
        assertThat(DescargadorDeCalendario.leerLimitado(new ByteArrayInputStream(justo)).length(),
                is(DescargadorDeCalendario.MAXIMO_BYTES));
    }
}
