package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.mappers.BloqueOcupadoMapper;

public class BloqueOcupadoMapperTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);

    private BloqueOcupadoEntity convertir(String motivo) {
        BloqueOcupadoDTORequest dto = new BloqueOcupadoDTORequest(FECHA, LocalTime.of(8, 0), LocalTime.of(15, 0),
                motivo);
        return BloqueOcupadoMapper.toEntity(dto, new PerfilCuidadorEntity());
    }

    @Test
    void testToEntityCopiaLaFechaYLasHoras() {
        BloqueOcupadoEntity bloque = convertir("Turno en el hospital");

        assertThat(bloque.getFecha(), is(equalTo(FECHA)));
        assertThat(bloque.getHoraInicio(), is(equalTo(LocalTime.of(8, 0))));
        assertThat(bloque.getHoraFin(), is(equalTo(LocalTime.of(15, 0))));
        assertThat(bloque.getMotivo(), is(equalTo("Turno en el hospital")));
    }

    @Test
    void testToEntityQuitaLosEspaciosDelMotivo() {
        assertThat(convertir("   Cita médica  ").getMotivo(), is(equalTo("Cita médica")));
    }

    @Test
    void testToEntityConMotivoEnBlancoNoGuardaMotivo() {
        assertThat(convertir("    ").getMotivo(), is(nullValue()));
    }

    @Test
    void testToEntityConMotivoNuloNoGuardaMotivo() {
        assertThat(convertir(null).getMotivo(), is(nullValue()));
    }

    @Test
    void testToDTOCopiaTodosLosCampos() {
        BloqueOcupadoEntity bloque = convertir("Turno");
        bloque.setId(7L);

        BloqueOcupadoDTOResponse dto = BloqueOcupadoMapper.toDTO(bloque);

        assertThat(dto.id(), is(equalTo(7L)));
        assertThat(dto.fecha(), is(equalTo(FECHA)));
        assertThat(dto.horaInicio(), is(equalTo(LocalTime.of(8, 0))));
        assertThat(dto.horaFin(), is(equalTo(LocalTime.of(15, 0))));
        assertThat(dto.motivo(), is(equalTo("Turno")));
    }
}
