package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import tools.jackson.databind.ObjectMapper;

// Tests de la lista de personas a cuidar en el controlador (validaciones y respuesta)
@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerPersonasTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SolicitudService service;
    @Autowired
    ObjectMapper mapper;

    // Petición con solo la lista de personas (sin nombrePaciente ni edadPaciente)
    private SolicitudDTORequest peticion(List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTORequest("domicilio", null, "Sin notas", null, LocalDate.now().plusDays(5), null, 1L,
                1L, null, null, null, LocalTime.of(9, 0), LocalTime.of(12, 0), personas);
    }

    private List<PersonaCuidadaDTO> lista(int cuantas) {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        for (int i = 1; i <= cuantas; i++) {
            personas.add(new PersonaCuidadaDTO("Persona " + i, 10 + i));
        }
        return personas;
    }

    private SolicitudDTOResponse respuesta(List<PersonaCuidadaDTO> personas) {
        return new SolicitudDTOResponse(1L, "domicilio", "Persona 1", "Sin notas", 11, LocalDate.of(2030, 1, 10), null,
                EstadoSolicitud.PENDIENTE, 1L, 1L, "Ana", "Pepe", new ArrayList<>(), null, null, LocalTime.of(9, 0),
                LocalTime.of(12, 0), personas);
    }

    private MockHttpServletResponse crear(SolicitudDTORequest dto) throws Exception {
        return mockMvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto))).andReturn().getResponse();
    }

    @Test
    void testStoreConTresPersonasPasaLaListaAlServicioYLaDevuelve() throws Exception {
        when(service.storeEntity(any(SolicitudDTORequest.class))).thenReturn(respuesta(lista(3)));

        MockHttpServletResponse response = crear(peticion(lista(3)));

        ArgumentCaptor<SolicitudDTORequest> captor = ArgumentCaptor.forClass(SolicitudDTORequest.class);
        verify(service).storeEntity(captor.capture());
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(captor.getValue().personas().size(), is(equalTo(3)));
        assertThat(response.getContentAsString(), containsString("\"personas\":["));
        assertThat(response.getContentAsString(), containsString("Persona 3"));
    }

    @Test
    void testStoreConCincoPersonasEsValido() throws Exception {
        when(service.storeEntity(any(SolicitudDTORequest.class))).thenReturn(respuesta(lista(5)));

        MockHttpServletResponse response = crear(peticion(lista(5)));

        assertThat(response.getStatus(), is(equalTo(201)));
    }

    @Test
    void testStoreConSeisPersonasDevuelve400() throws Exception {
        MockHttpServletResponse response = crear(peticion(lista(6)));

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(SolicitudDTORequest.class));
    }

    @Test
    void testStoreConUnaPersonaSinNombreDevuelve400() throws Exception {
        List<PersonaCuidadaDTO> personas = lista(2);
        personas.add(new PersonaCuidadaDTO("  ", 30));

        MockHttpServletResponse response = crear(peticion(personas));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("El nombre de la persona a cuidar no puede estar vacio"));
        verify(service, never()).storeEntity(any(SolicitudDTORequest.class));
    }

    @Test
    void testStoreConUnaPersonaSinEdadDevuelve400() throws Exception {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Carmen", null));

        MockHttpServletResponse response = crear(peticion(personas));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La edad de la persona a cuidar no puede ser nula"));
    }

    @Test
    void testStoreConEdadNegativaEnUnaPersonaDevuelve400() throws Exception {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Carmen", -1));

        MockHttpServletResponse response = crear(peticion(personas));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La edad no puede ser negativa"));
    }

    @Test
    void testStoreConEdadImposibleEnUnaPersonaDevuelve400() throws Exception {
        List<PersonaCuidadaDTO> personas = new ArrayList<>();
        personas.add(new PersonaCuidadaDTO("Carmen", 121));

        MockHttpServletResponse response = crear(peticion(personas));

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La edad no puede superar 120 años"));
    }

    @Test
    void testStoreSinListaPeroConNombreYEdadSueltosSigueFuncionando() throws Exception {
        SolicitudDTORequest dto = new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 80,
                LocalDate.now().plusDays(5), null, 1L, 1L);
        when(service.storeEntity(any(SolicitudDTORequest.class))).thenReturn(respuesta(lista(1)));

        MockHttpServletResponse response = crear(dto);

        assertThat(response.getStatus(), is(equalTo(201)));
    }

    @Test
    void testUpdateConSeisPersonasDevuelve400() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/solicitudes/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(peticion(lista(6)))))
                .andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
    }
}
