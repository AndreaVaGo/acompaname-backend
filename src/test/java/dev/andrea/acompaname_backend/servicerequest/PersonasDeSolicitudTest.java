package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionPersonasInvalidas;

// Tests de las reglas de la lista de personas a cuidar (entre 1 y 5)
public class PersonasDeSolicitudTest {

    // Petición con la lista de personas y el nombre y edad sueltos que le pasemos
    private SolicitudDTORequest peticion(String nombre, Integer edad, List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("domicilio", nombre, "Sin notas", edad, LocalDate.of(2030, 1, 10), null, 1L,
                1L, null, null, null, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    private List<PersonaCuidadaDTO> lista(int cuantas) {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        for (int i = 1; i <= cuantas; i++) {
            personas.add(new PersonaCuidadaDTO("Persona " + i, 10 + i));
        }
        return personas;
    }

    @Test
    void testUnaListaDeTresPersonasSeRespetaEnOrden() {
        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(null, null, lista(3)));

        assertThat(resultado.size(), is(equalTo(3)));
        assertThat(resultado.get(0).getNombre(), is(equalTo("Persona 1")));
        assertThat(resultado.get(2).getNombre(), is(equalTo("Persona 3")));
        assertThat(resultado.get(2).getEdad(), is(equalTo(13)));
    }

    @Test
    void testCincoPersonasEsElMaximoPermitido() {
        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(null, null, lista(5)));

        assertThat(resultado.size(), is(equalTo(5)));
    }

    @Test
    void testSeisPersonasDaError() {
        SolicitudDTORequest dto = peticion(null, null, lista(6));

        SolicitudExceptionPersonasInvalidas error = assertThrows(SolicitudExceptionPersonasInvalidas.class,
                () -> PersonasDeSolicitud.desdePeticion(dto));
        assertThat(error.getMessage(), containsString("más de 5"));
    }

    @Test
    void testSinListaNiNombreDaError() {
        SolicitudDTORequest dto = peticion(null, null, null);

        assertThrows(SolicitudExceptionPersonasInvalidas.class, () -> PersonasDeSolicitud.desdePeticion(dto));
    }

    @Test
    void testListaVaciaYNombreEnBlancoDaError() {
        SolicitudDTORequest dto = peticion("   ", 80, new ArrayList<>());

        assertThrows(SolicitudExceptionPersonasInvalidas.class, () -> PersonasDeSolicitud.desdePeticion(dto));
    }

    @Test
    void testSinListaPeroConNombreYEdadSueltosEsUnaSolaPersona() {
        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion("Manuel", 80, null));

        assertThat(resultado.size(), is(equalTo(1)));
        assertThat(resultado.get(0).getNombre(), is(equalTo("Manuel")));
        assertThat(resultado.get(0).getEdad(), is(equalTo(80)));
    }

    @Test
    void testSiHayListaSeIgnoranElNombreYLaEdadSueltos() {
        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion("Otro", 99, lista(2)));

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(0).getNombre(), is(equalTo("Persona 1")));
    }

    @Test
    void testLosNombresSeGuardanSinEspaciosAlPrincipioNiAlFinal() {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("  Carmen  ", 82));

        List<PersonaCuidada> resultado = PersonasDeSolicitud.desdePeticion(peticion(null, null, personas));

        assertThat(resultado.get(0).getNombre(), is(equalTo("Carmen")));
    }

    @Test
    void testUnaSolicitudAntiguaSinListaCuentaComoUnaPersona() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setNombrePaciente("Manuel");
        solicitud.setEdadPaciente(75);

        List<PersonaCuidadaDTO> resultado = PersonasDeSolicitud.aDTO(solicitud);

        assertThat(resultado.size(), is(equalTo(1)));
        assertThat(resultado.get(0).nombre(), is(equalTo("Manuel")));
        assertThat(resultado.get(0).edad(), is(equalTo(75)));
    }

    @Test
    void testUnaSolicitudSinNingunDatoDevuelveListaVacia() {
        List<PersonaCuidadaDTO> resultado = PersonasDeSolicitud.aDTO(new SolicitudEntity());

        assertThat(resultado.size(), is(equalTo(0)));
    }

    @Test
    void testUnaSolicitudConListaDevuelveTodasLasPersonas() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setNombrePaciente("Carmen");
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        List<PersonaCuidadaDTO> resultado = PersonasDeSolicitud.aDTO(solicitud);

        assertThat(resultado.size(), is(equalTo(2)));
        assertThat(resultado.get(1).nombre(), is(equalTo("Antonio")));
    }
}
