package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

public class BloqueOcupadoEntityTest {

    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    // Bloque de ese día de 08:00 a 15:00
    private BloqueOcupadoEntity bloque() {
        return BloqueOcupadoEntity.builder()
                .fecha(DIA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(15, 0))
                .build();
    }

    private LocalDateTime a(int hora) {
        return DIA.atTime(hora, 0);
    }

    @Test
    void testMomentoInicioYFinUnenLaFechaConLasHoras() {
        assertThat(bloque().momentoInicio(), is(equalTo(DIA.atTime(8, 0))));
        assertThat(bloque().momentoFin(), is(equalTo(DIA.atTime(15, 0))));
    }

    @Test
    void testSeCruzaConUnRangoQueEmpiezaDentro() {
        assertThat(bloque().seCruzaCon(a(14), a(18)), is(true));
    }

    @Test
    void testSeCruzaConUnRangoQueAcabaDentro() {
        assertThat(bloque().seCruzaCon(a(6), a(9)), is(true));
    }

    @Test
    void testSeCruzaConUnRangoQueLoContieneEntero() {
        assertThat(bloque().seCruzaCon(a(6), a(20)), is(true));
    }

    @Test
    void testSeCruzaConUnRangoQueEstaDentro() {
        assertThat(bloque().seCruzaCon(a(10), a(11)), is(true));
    }

    @Test
    void testNoSeCruzaConUnRangoPosterior() {
        assertThat(bloque().seCruzaCon(a(16), a(18)), is(false));
    }

    @Test
    void testNoSeCruzaConUnRangoAnterior() {
        assertThat(bloque().seCruzaCon(a(5), a(7)), is(false));
    }

    @Test
    void testSiUnoAcabaJustoCuandoEmpiezaElOtroNoSeCruzan() {
        assertThat(bloque().seCruzaCon(a(15), a(18)), is(false));
        assertThat(bloque().seCruzaCon(a(5), a(8)), is(false));
    }

    @Test
    void testNoSeCruzaConOtroDia() {
        LocalDateTime inicio = DIA.plusDays(1).atTime(9, 0);
        assertThat(bloque().seCruzaCon(inicio, inicio.plusHours(2)), is(false));
    }

    @Test
    void testSeCruzaConUnServicioDeVariosDiasQuePasaPorEseDia() {
        // Del día anterior a las 20:00 al día siguiente a las 08:00 (que justo acaba cuando empieza el bloque)
        assertThat(bloque().seCruzaCon(DIA.minusDays(1).atTime(20, 0), DIA.plusDays(1).atTime(8, 0)), is(true));
    }
}
