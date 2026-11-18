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
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests del grupo de edad de cada persona en el mapper
public class SolicitudMapperPersonasGrupoTest {

    private PerfilCuidadorEntity crearCuidador() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, usuario);
    }

    private UsuarioEntity crearFamilia() {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setId(1L);
        familia.setNombre("Ana");
        return familia;
    }

    private List<PersonaCuidadaDTO> tresNinosYUnAbuelo() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Zoe", 5, GrupoEdad.NINOS));
        personas.add(new PersonaCuidadaDTO("Ada", 9, GrupoEdad.NINOS));
        personas.add(new PersonaCuidadaDTO("Leo", 11, GrupoEdad.NINOS));
        personas.add(new PersonaCuidadaDTO("Abuelo", 80, GrupoEdad.MAYORES));
        return personas;
    }

    private SolicitudDTORequest peticion(GrupoEdad grupo, List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("domicilio", null, "Sin notas", null, LocalDate.of(2030, 1, 10), null, 1L, 1L,
                null, null, grupo, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    @Test
    void testToEntityGuardaElGrupoDeCadaPersona() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(peticion(null, tresNinosYUnAbuelo()), crearFamilia(),
                crearCuidador());

        assertThat(solicitud.getPersonas().get(0).getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
        assertThat(solicitud.getPersonas().get(3).getGrupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testElGrupoDeLaSolicitudEsElDeLaPrimeraPersona() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(peticion(GrupoEdad.MAYORES, tresNinosYUnAbuelo()),
                crearFamilia(), crearCuidador());

        assertThat(solicitud.getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testToDtoDevuelveElGrupoDeCadaPersona() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(peticion(null, tresNinosYUnAbuelo()), crearFamilia(),
                crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.personas().size(), is(equalTo(4)));
        assertThat(dto.personas().get(3).grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
        assertThat(dto.grupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }
}
