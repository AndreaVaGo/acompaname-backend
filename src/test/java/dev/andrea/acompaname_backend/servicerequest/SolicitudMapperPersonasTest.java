package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests de la lista de personas a cuidar en el mapper
public class SolicitudMapperPersonasTest {

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

    private SolicitudDTORequest crearRequest(List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("domicilio", null, "Sin notas", null, LocalDate.of(2030, 1, 10), null, 1L, 1L,
                null, null, null, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    private List<PersonaCuidadaDTO> matrimonio() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Carmen", 82));
        personas.add(new PersonaCuidadaDTO("Antonio", 85));
        return personas;
    }

    @Test
    void testToEntityGuardaTodasLasPersonas() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(matrimonio()), crearFamilia(),
                crearCuidador());

        assertThat(solicitud.getPersonas().size(), is(equalTo(2)));
        assertThat(solicitud.getPersonas().get(1).getNombre(), is(equalTo("Antonio")));
        assertThat(solicitud.getPersonas().get(1).getEdad(), is(equalTo(85)));
    }

    @Test
    void testToEntityCopiaLaPrimeraPersonaAlNombreYEdadDelPaciente() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(matrimonio()), crearFamilia(),
                crearCuidador());

        assertThat(solicitud.getNombrePaciente(), is(equalTo("Carmen")));
        assertThat(solicitud.getEdadPaciente(), is(equalTo(82)));
    }

    @Test
    void testToEntityConLaFormaAntiguaCreaUnaSolaPersona() {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2030, 1, 10), null, 1L, 1L);

        SolicitudEntity solicitud = SolicitudMapper.toEntity(dto, crearFamilia(), crearCuidador());

        assertThat(solicitud.getPersonas().size(), is(equalTo(1)));
        assertThat(solicitud.getPersonas().get(0).getNombre(), is(equalTo("Manuel")));
        assertThat(solicitud.getNombrePaciente(), is(equalTo("Manuel")));
    }

    @Test
    void testToDtoDevuelveLaListaDePersonas() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(matrimonio()), crearFamilia(),
                crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.personas().size(), is(equalTo(2)));
        assertThat(dto.personas().get(0).nombre(), is(equalTo("Carmen")));
        assertThat(dto.personas().get(1).edad(), is(equalTo(85)));
        assertThat(dto.nombrePaciente(), is(equalTo("Carmen")));
    }

    @Test
    void testToDtoDeUnaSolicitudAntiguaDevuelveUnaPersona() {
        SolicitudEntity antigua = new SolicitudEntity(1L, "domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, crearFamilia(), crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(antigua);

        assertThat(dto.personas().size(), is(equalTo(1)));
        assertThat(dto.personas().get(0).nombre(), is(equalTo("Manuel")));
        assertThat(dto.personas().get(0).edad(), is(equalTo(80)));
    }

    @Test
    void testLaRespuestaConstruidaSinListaTieneUnaPersona() {
        SolicitudDTOResponse dto = new SolicitudDTOResponse(1L, "domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2030, 1, 10), EstadoSolicitud.PENDIENTE, 1L, 1L, "Ana", "Pepe");

        assertThat(dto.personas().size(), is(equalTo(1)));
        assertThat(dto.personas().get(0).nombre(), is(equalTo("Manuel")));
    }
}
