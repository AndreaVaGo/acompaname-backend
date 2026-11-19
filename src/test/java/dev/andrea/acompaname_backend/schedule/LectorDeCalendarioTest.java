package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;

// Lee archivos .ics de ejemplo y comprueba qué ratos ocupados salen de cada uno
public class LectorDeCalendarioTest {

    private final LectorDeCalendario lector = new LectorDeCalendario();

    // Monta un calendario con los eventos que se le pasen (cada evento son sus líneas)
    private String calendario(String... eventos) {
        StringBuilder texto = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n");
        for (String evento : eventos) {
            texto.append("BEGIN:VEVENT\r\n").append(evento).append("\r\nEND:VEVENT\r\n");
        }
        texto.append("END:VCALENDAR\r\n");
        return texto.toString();
    }

    private String lineas(String... lineas) {
        return String.join("\r\n", lineas);
    }

    @Test
    void testUnEventoEnUtcSePasaALaHoraDeMadrid() {
        // En diciembre Madrid va una hora por delante de UTC
        String ics = calendario(lineas("SUMMARY:Cita", "DTSTART:20261201T080000Z", "DTEND:20261201T100000Z"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(1)));
        TramoOcupado tramo = lectura.tramos().get(0);
        assertThat(tramo.fecha(), is(equalTo(LocalDate.of(2026, 12, 1))));
        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(11, 0))));
        assertThat(lectura.omitidos(), is(equalTo(0)));
    }

    @Test
    void testEnVeranoMadridVaDosHorasPorDelanteDeUtc() {
        String ics = calendario(lineas("DTSTART:20260715T080000Z", "DTEND:20260715T100000Z"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(10, 0))));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(12, 0))));
    }

    @Test
    void testUnEventoConZonaDeMadridConservaSuHora() {
        String ics = calendario(lineas("DTSTART;TZID=Europe/Madrid:20261201T090000",
                "DTEND;TZID=Europe/Madrid:20261201T110000"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(11, 0))));
    }

    @Test
    void testUnEventoConOtraZonaSePasaALaHoraDeMadrid() {
        // Nueva York va 6 horas por detrás de Madrid: las 9:00 allí son las 15:00 aquí
        String ics = calendario(lineas("DTSTART;TZID=America/New_York:20261201T090000",
                "DTEND;TZID=America/New_York:20261201T100000"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(15, 0))));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(16, 0))));
    }

    @Test
    void testUnaZonaQueJavaNoConoceSeTomaTalCual() {
        String ics = calendario(lineas("DTSTART;TZID=Romance Standard Time:20261201T090000",
                "DTEND;TZID=Romance Standard Time:20261201T110000"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(9, 0))));
    }

    @Test
    void testUnEventoSinZonaSeTomaComoHoraLocal() {
        String ics = calendario(lineas("DTSTART:20261201T090000", "DTEND:20261201T110000"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(11, 0))));
    }

    @Test
    void testUnEventoDeTodoElDiaOcupaDeLas0000ALas2359() {
        // En los eventos de todo el día, la fecha de fin es la del día siguiente
        String ics = calendario(lineas("DTSTART;VALUE=DATE:20261201", "DTEND;VALUE=DATE:20261202"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(1)));
        TramoOcupado tramo = lectura.tramos().get(0);
        assertThat(tramo.fecha(), is(equalTo(LocalDate.of(2026, 12, 1))));
        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.MIDNIGHT)));
        assertThat(tramo.horaFin(), is(equalTo(LocalTime.of(23, 59))));
    }

    @Test
    void testUnEventoDeTodoElDiaSinFinDuraEseDia() {
        String ics = calendario(lineas("DTSTART;VALUE=DATE:20261201"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(1)));
        assertThat(lectura.tramos().get(0).fecha(), is(equalTo(LocalDate.of(2026, 12, 1))));
    }

    @Test
    void testUnEventoDeVariosDiasSePartePorDias() {
        String ics = calendario(lineas("DTSTART:20261130T220000", "DTEND:20261202T080000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(3)));
        assertThat(lectura.tramos().get(0), is(equalTo(
                new TramoOcupado(LocalDate.of(2026, 11, 30), LocalTime.of(22, 0), LocalTime.of(23, 59)))));
        assertThat(lectura.tramos().get(1), is(equalTo(
                new TramoOcupado(LocalDate.of(2026, 12, 1), LocalTime.MIDNIGHT, LocalTime.of(23, 59)))));
        assertThat(lectura.tramos().get(2), is(equalTo(
                new TramoOcupado(LocalDate.of(2026, 12, 2), LocalTime.MIDNIGHT, LocalTime.of(8, 0)))));
    }

    @Test
    void testUnEventoQueAcabaALasDoceDeLaNocheNoOcupaElDiaSiguiente() {
        String ics = calendario(lineas("DTSTART:20261201T200000", "DTEND:20261202T000000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(1)));
        assertThat(lectura.tramos().get(0), is(equalTo(
                new TramoOcupado(LocalDate.of(2026, 12, 1), LocalTime.of(20, 0), LocalTime.of(23, 59)))));
    }

    @Test
    void testUnEventoRepetidoSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T090000", "DTEND:20261201T110000",
                "RRULE:FREQ=WEEKLY;COUNT=10"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoCanceladoSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T090000", "DTEND:20261201T110000", "STATUS:CANCELLED"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoMarcadoComoLibreSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T090000", "DTEND:20261201T110000", "TRANSP:TRANSPARENT"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoConHoraSinFinSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T090000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoSinInicioSeOmite() {
        String ics = calendario(lineas("SUMMARY:Sin fecha"));

        assertThat(lector.leer(ics).omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoCuyoFinEsAnteriorAlInicioSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T110000", "DTEND:20261201T090000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnaFechaQueNoSeEntiendeSeOmiteSinFallar() {
        String ics = calendario(lineas("DTSTART:esto-no-es-una-fecha", "DTEND:20261201T110000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnEventoDeMasDeUnMesSeOmite() {
        String ics = calendario(lineas("DTSTART:20261201T090000", "DTEND:20270301T090000"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testLasLineasPartidasSeVuelvenAJuntar() {
        // Una línea larga puede venir partida: la siguiente empieza por un espacio
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nDTSTART:20261201T09\r\n 0000Z\r\n"
                + "DTEND:20261201T100000Z\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.horaInicio(), is(equalTo(LocalTime.of(10, 0))));
    }

    @Test
    void testTambienSeLeenLosSaltosDeLineaSencillos() {
        String ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART:20261201T090000\nDTEND:20261201T100000\n"
                + "END:VEVENT\nEND:VCALENDAR\n";

        assertThat(lector.leer(ics).tramos().size(), is(equalTo(1)));
    }

    @Test
    void testLasHorasDeLaDefinicionDeZonaHorariaNoCuentanComoEventos() {
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VTIMEZONE\r\nTZID:Europe/Madrid\r\nBEGIN:STANDARD\r\n"
                + "DTSTART:19701025T030000\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n"
                + "BEGIN:VEVENT\r\nDTSTART:20261201T090000\r\nDTEND:20261201T100000\r\nEND:VEVENT\r\n"
                + "END:VCALENDAR\r\n";

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(1)));
        assertThat(lectura.omitidos(), is(equalTo(0)));
    }

    @Test
    void testVariosEventosSeLeenTodos() {
        String ics = calendario(
                lineas("DTSTART:20261201T090000", "DTEND:20261201T100000"),
                lineas("DTSTART:20261202T090000", "DTEND:20261202T100000"),
                lineas("DTSTART:20261203T090000", "DTEND:20261203T100000", "RRULE:FREQ=DAILY"));

        LecturaDeCalendario lectura = lector.leer(ics);

        assertThat(lectura.tramos().size(), is(equalTo(2)));
        assertThat(lectura.omitidos(), is(equalTo(1)));
    }

    @Test
    void testUnCalendarioSinEventosDaUnaListaVacia() {
        LecturaDeCalendario lectura = lector.leer("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n");

        assertThat(lectura.tramos(), is(empty()));
        assertThat(lectura.omitidos(), is(equalTo(0)));
    }

    @Test
    void testElTituloNoSeGuardaEnNingunSitio() {
        // El bloque solo tiene fecha y horas: no hay dónde guardar el título de la cita
        String ics = calendario(lineas("SUMMARY:Revisión médica privada", "DESCRIPTION:Cosas privadas",
                "DTSTART:20261201T090000", "DTEND:20261201T100000"));

        TramoOcupado tramo = lector.leer(ics).tramos().get(0);

        assertThat(tramo.toString().contains("privad"), is(false));
    }

    @Test
    void testUnTextoQueNoEsUnCalendarioDaError() {
        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> lector.leer("hola, esto no es un calendario"));
    }

    @Test
    void testUnTextoNuloDaError() {
        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> lector.leer(null));
    }

    @Test
    void testDemasiadosEventosDaError() {
        StringBuilder ics = new StringBuilder("BEGIN:VCALENDAR\r\n");
        for (int i = 0; i <= LectorDeCalendario.MAXIMO_TRAMOS; i++) {
            LocalDate dia = LocalDate.of(2027, 1, 1).plusDays(i);
            String fecha = dia.toString().replace("-", "");
            ics.append("BEGIN:VEVENT\r\nDTSTART:").append(fecha).append("T090000\r\nDTEND:").append(fecha)
                    .append("T100000\r\nEND:VEVENT\r\n");
        }
        ics.append("END:VCALENDAR\r\n");

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> lector.leer(ics.toString()));
    }
}
