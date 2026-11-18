package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.servicerequest.PersonaCuidada;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;

// Comprueba el cálculo del importe: horas x tarifa, con recargo por cada persona extra
public class CalculoDeImporteTest {

    private static final BigDecimal TARIFA = new BigDecimal("15.00");

    private CalculoDeImporte calculo;

    @BeforeEach
    void setup() {
        calculo = new CalculoDeImporte(new BigDecimal("0.25"));
    }

    // Solicitud de un día, de la hora de inicio a la hora de fin
    private SolicitudEntity unDia(LocalTime inicio, LocalTime fin) {
        return SolicitudEntity.builder()
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .horaInicio(inicio)
                .horaFin(fin)
                .build();
    }

    @Test
    void testUnaPersonaCuatroHorasEsHorasPorTarifa() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("60.00"))));
    }

    @Test
    void testUnaFraccionDeHoraCuentaComoHoraEntera() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(12, 30));

        // 2 h 30 min cuentan como 3 horas
        assertThat(calculo.horasDe(solicitud), is(equalTo(3L)));
        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("45.00"))));
    }

    @Test
    void testElMinimoEsUnaHora() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(10, 20));

        assertThat(calculo.horasDe(solicitud), is(equalTo(1L)));
    }

    @Test
    void testVariosDiasCuentanDesdeLaHoraDeInicioHastaLaHoraDeFin() {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .fechaFin(LocalDate.of(2026, 12, 16))
                .horaInicio(LocalTime.of(20, 0))
                .horaFin(LocalTime.of(8, 0))
                .build();

        // De las 20:00 del primer día a las 08:00 del segundo: 12 horas
        assertThat(calculo.horasDe(solicitud), is(equalTo(12L)));
        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("180.00"))));
    }

    @Test
    void testSinHorasElCuidadoCuentaComoElDiaCompleto() {
        SolicitudEntity solicitud = unDia(null, null);

        assertThat(calculo.horasDe(solicitud), is(equalTo(24L)));
    }

    @Test
    void testUnaPersonaExtraSumaElRecargo() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(14, 0));
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        // 4 x 15 x (1 + 0,25) = 75,00
        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("75.00"))));
    }

    @Test
    void testTresPersonasSumanDosRecargos() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(14, 0));
        solicitud.getPersonas().add(new PersonaCuidada("Ana", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Luis", 7));
        solicitud.getPersonas().add(new PersonaCuidada("Abuelo", 80));

        // 4 x 15 x (1 + 0,25 x 2) = 90,00
        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("90.00"))));
    }

    @Test
    void testUnaSolicitudAntiguaSinListaCuentaComoUnaPersona() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(14, 0));
        solicitud.setNombrePaciente("Manuel");

        assertThat(solicitud.numeroDePersonas(), is(equalTo(1)));
        assertThat(calculo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("60.00"))));
    }

    @Test
    void testSinRecargoLasPersonasNoCambianElImporte() {
        CalculoDeImporte sinRecargo = new CalculoDeImporte(BigDecimal.ZERO);
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(14, 0));
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        assertThat(sinRecargo.calcular(solicitud, TARIFA), is(equalTo(new BigDecimal("60.00"))));
    }

    @Test
    void testElImporteSeRedondeaADosDecimales() {
        SolicitudEntity solicitud = unDia(LocalTime.of(10, 0), LocalTime.of(13, 0));

        // 3 x 14,555 = 43,665 -> 43,67
        assertThat(calculo.calcular(solicitud, new BigDecimal("14.555")), is(equalTo(new BigDecimal("43.67"))));
    }
}
