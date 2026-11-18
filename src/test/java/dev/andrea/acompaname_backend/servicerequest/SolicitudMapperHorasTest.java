package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests de la hora de inicio y de fin de la solicitud en el mapper
public class SolicitudMapperHorasTest {

    private UsuarioEntity crearFamilia() {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setId(1L);
        familia.setNombre("Ana");
        familia.setEmail("ana@test.com");
        return familia;
    }

    private PerfilCuidadorEntity crearCuidador() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, usuario);
    }

    private SolicitudDTORequest crearRequest(LocalTime inicio, LocalTime fin) {
        return new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80, LocalDate.of(2026, 11, 10), null, 1L,
                1L, null, null, null, inicio, fin);
    }

    @Test
    void testToEntityGuardaLasHoras() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(LocalTime.of(9, 0), LocalTime.of(12, 30)),
                crearFamilia(), crearCuidador());

        assertThat(solicitud.getHoraInicio(), is(equalTo(LocalTime.of(9, 0))));
        assertThat(solicitud.getHoraFin(), is(equalTo(LocalTime.of(12, 30))));
    }

    @Test
    void testToDtoDevuelveLasHoras() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(LocalTime.of(15, 0), LocalTime.of(18, 0)),
                crearFamilia(), crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.horaInicio(), is(equalTo(LocalTime.of(15, 0))));
        assertThat(dto.horaFin(), is(equalTo(LocalTime.of(18, 0))));
    }

    @Test
    void testUnaSolicitudAntiguaSinHorasDevuelveNull() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                crearFamilia(), crearCuidador());
        solicitud.setHoraInicio(null);
        solicitud.setHoraFin(null);

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.horaInicio(), is(nullValue()));
        assertThat(dto.horaFin(), is(nullValue()));
    }

    @Test
    void testLaPeticionSinHorasEsDeDiaCompleto() {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 11, 10), null, 1L, 1L);

        assertThat(dto.horaInicio(), is(equalTo(LocalTime.of(0, 0))));
        assertThat(dto.horaFin(), is(equalTo(LocalTime.of(23, 59))));
    }
}
