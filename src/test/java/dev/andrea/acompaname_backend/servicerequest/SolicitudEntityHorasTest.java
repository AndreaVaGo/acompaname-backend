package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

// Tests de cuándo empieza, cuándo acaba y cuándo se cruzan dos cuidados
public class SolicitudEntityHorasTest {

    private static final LocalDate DIA = LocalDate.of(2026, 9, 10);

    private SolicitudEntity solicitud(LocalDate fin, LocalTime horaInicio, LocalTime horaFin) {
        return SolicitudEntity.builder()
                .fechaCuidado(DIA)
                .fechaFin(fin)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .build();
    }

    private LocalDateTime hoy(int hora) {
        return DIA.atTime(hora, 0);
    }

    @Test
    void testElMomentoDeInicioUsaLaFechaYLaHoraDeInicio() {
        SolicitudEntity solicitud = solicitud(null, LocalTime.of(9, 30), LocalTime.of(12, 0));

        assertThat(solicitud.momentoInicio(), is(equalTo(LocalDateTime.of(2026, 9, 10, 9, 30))));
    }

    @Test
    void testElMomentoDeFinUsaLaFechaDeInicioSiNoHayFechaFin() {
        SolicitudEntity solicitud = solicitud(null, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(solicitud.momentoFin(), is(equalTo(LocalDateTime.of(2026, 9, 10, 12, 0))));
    }

    @Test
    void testElMomentoDeFinUsaLaFechaFinYSuHoraSiLaHay() {
        SolicitudEntity solicitud = solicitud(LocalDate.of(2026, 9, 12), LocalTime.of(22, 0), LocalTime.of(8, 0));

        assertThat(solicitud.momentoFin(), is(equalTo(LocalDateTime.of(2026, 9, 12, 8, 0))));
    }

    @Test
    void testUnaSolicitudAntiguaSinHorasEmpiezaALas00YAcabaAFinalDelDia() {
        SolicitudEntity solicitud = solicitud(null, null, null);

        assertThat(solicitud.momentoInicio(), is(equalTo(DIA.atStartOfDay())));
        assertThat(solicitud.momentoFin(), is(equalTo(DIA.atTime(LocalTime.MAX))));
    }

    @Test
    void testDosCuidadosCuyasHorasSeMezclanSeCruzan() {
        SolicitudEntity deNueveADoce = solicitud(null, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(deNueveADoce.seCruzaCon(hoy(11), hoy(14)), is(true));
        assertThat(deNueveADoce.seCruzaCon(hoy(8), hoy(10)), is(true));
    }

    @Test
    void testUnCuidadoQueContieneAOtroSeCruza() {
        SolicitudEntity deNueveADoce = solicitud(null, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(deNueveADoce.seCruzaCon(hoy(10), hoy(11)), is(true));
        assertThat(deNueveADoce.seCruzaCon(hoy(8), hoy(13)), is(true));
    }

    @Test
    void testDosCuidadosDeHorasSeparadasNoSeCruzan() {
        SolicitudEntity deNueveADoce = solicitud(null, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(deNueveADoce.seCruzaCon(hoy(15), hoy(18)), is(false));
        assertThat(deNueveADoce.seCruzaCon(hoy(6), hoy(8)), is(false));
    }

    @Test
    void testSiUnoAcabaJustoCuandoEmpiezaElOtroNoSeCruzan() {
        SolicitudEntity deNueveADoce = solicitud(null, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(deNueveADoce.seCruzaCon(hoy(12), hoy(15)), is(false));
        assertThat(deNueveADoce.seCruzaCon(hoy(6), hoy(9)), is(false));
    }

    @Test
    void testUnaSolicitudAntiguaSinHorasSeCruzaConCualquierHoraDeESeDia() {
        SolicitudEntity diaCompleto = solicitud(null, null, null);

        assertThat(diaCompleto.seCruzaCon(hoy(15), hoy(18)), is(true));
    }

    @Test
    void testUnCuidadoDeVariosDiasNoSeCruzaConLaMananaDelUltimoDiaSiAcabaAntes() {
        // Del 10 a las 22:00 al 11 a las 08:00
        SolicitudEntity deNoche = solicitud(LocalDate.of(2026, 9, 11), LocalTime.of(22, 0), LocalTime.of(8, 0));

        assertThat(deNoche.seCruzaCon(LocalDateTime.of(2026, 9, 11, 9, 0), LocalDateTime.of(2026, 9, 11, 12, 0)),
                is(false));
        assertThat(deNoche.seCruzaCon(LocalDateTime.of(2026, 9, 11, 7, 0), LocalDateTime.of(2026, 9, 11, 10, 0)),
                is(true));
        assertThat(deNoche.seCruzaCon(LocalDateTime.of(2026, 9, 10, 20, 0), LocalDateTime.of(2026, 9, 10, 23, 0)),
                is(true));
    }
}
