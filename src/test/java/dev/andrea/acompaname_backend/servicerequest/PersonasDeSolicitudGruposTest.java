package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;

// Tests del grupo de edad de cada persona de la solicitud (por ejemplo, tres niños y un abuelo)
public class PersonasDeSolicitudGruposTest {

    private SolicitudDTORequest peticion(GrupoEdad grupoDeLaSolicitud, List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("domicilio", null, "Sin notas", null, LocalDate.of(2030, 1, 10), null, 1L, 1L,
                null, null, grupoDeLaSolicitud, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    @Test
    void testCadaPersonaGuardaSuPropioGrupo() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Mateo", 8, GrupoEdad.NINOS));
        personas.add(new PersonaCuidadaDTO("Abuelo", 80, GrupoEdad.MAYORES));

        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(GrupoEdad.NINOS, personas));

        assertThat(resultado.get(0).getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
        assertThat(resultado.get(1).getGrupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testUnaPersonaSinGrupoUsaElDeLaSolicitud() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Mateo", 8));

        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(GrupoEdad.NINOS, personas));

        assertThat(resultado.get(0).getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testSinGrupoEnNingunSitioQuedaNull() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Mateo", 8));

        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(null, personas));

        assertThat(resultado.get(0).getGrupoEdad(), is(nullValue()));
    }

    @Test
    void testLaFormaAntiguaUsaElGrupoDeLaSolicitud() {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2030, 1, 10), null, 1L, 1L, null, null, GrupoEdad.MAYORES);

        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(dto);

        assertThat(resultado.get(0).getGrupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testGruposDeNoRepiteLosIguales() {
        List<PersonaCuidada> personas = new ArrayList<>();
        personas.add(new PersonaCuidada("Zoe", 5, GrupoEdad.NINOS));
        personas.add(new PersonaCuidada("Ada", 9, GrupoEdad.NINOS));
        personas.add(new PersonaCuidada("Abuelo", 80, GrupoEdad.MAYORES));

        List<GrupoEdad> grupos = PersonasDeSolicitud.gruposDe(personas);

        assertThat(grupos.size(), is(equalTo(2)));
        assertThat(grupos.get(0), is(equalTo(GrupoEdad.NINOS)));
        assertThat(grupos.get(1), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testGruposDeIgnoraLasPersonasSinGrupo() {
        List<PersonaCuidada> personas = new ArrayList<>();
        personas.add(new PersonaCuidada("Zoe", 5));

        assertThat(PersonasDeSolicitud.gruposDe(personas).size(), is(equalTo(0)));
    }

    @Test
    void testUnaSolicitudAntiguaDevuelveSuGrupoEnLaPersona() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setNombrePaciente("Manuel");
        solicitud.setEdadPaciente(80);
        solicitud.setGrupoEdad(GrupoEdad.MAYORES);

        List<PersonaCuidadaDTO> resultado = PersonasDeSolicitud.aDTO(solicitud);

        assertThat(resultado.get(0).grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testUnaPersonaSinGrupoEnLaListaUsaElGrupoDeLaSolicitud() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setGrupoEdad(GrupoEdad.NINOS);
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Abuelo", 80, GrupoEdad.MAYORES));

        List<PersonaCuidadaDTO> resultado = PersonasDeSolicitud.aDTO(solicitud);

        assertThat(resultado.get(0).grupoEdad(), is(equalTo(GrupoEdad.NINOS)));
        assertThat(resultado.get(1).grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testLaEtiquetaDelGrupoSeLeeBien() {
        assertThat(GrupoEdad.MAYORES.etiqueta(), is(equalTo("Personas mayores")));
        assertThat(GrupoEdad.NINOS.etiqueta(), is(equalTo("Niñas y niños")));
        assertThat(GrupoEdad.BEBES.name(), is(equalTo("BEBES")));
    }
}
