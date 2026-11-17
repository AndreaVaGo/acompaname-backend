package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
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

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.globals.Titulacion;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PerfilCuidadorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilCuidadorControllerTitulacionesTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PerfilCuidadorService service;
    @Autowired
    ObjectMapper mapper;

    // Petición con las titulaciones y las otras titulaciones que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<Titulacion> titulaciones, String otras) {
        return new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L, null,
                null, null, null, null, null, titulaciones, otras);
    }

    // Respuesta con las titulaciones y las otras titulaciones que le pasemos
    private PerfilCuidadorDTOResponse crearResponse(List<Titulacion> titulaciones, String otras) {
        return new PerfilCuidadorDTOResponse(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L,
                "Pepe", null, null, 0L, "ambos", new ArrayList<>(), null, null, new ArrayList<GrupoEdad>(),
                titulaciones, otras);
    }

    @Test
    void testGetByIdDevuelveLasTitulaciones() throws Exception {
        when(service.getById(1L)).thenReturn(
                crearResponse(List.of(Titulacion.ENFERMERIA, Titulacion.PRIMEROS_AUXILIOS), "Curso de geriatría"));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(),
                containsString("\"titulaciones\":[\"ENFERMERIA\",\"PRIMEROS_AUXILIOS\"]"));
        assertThat(response.getContentAsString(), containsString("\"otrasTitulaciones\":\"Curso de geriatría\""));
    }

    @Test
    void testStorePasaLasTitulacionesAlServicio() throws Exception {
        PerfilCuidadorDTORequest dto = crearRequest(List.of(Titulacion.AUXILIAR_ENFERMERIA), "Otra formación");
        when(service.storeEntity(any(PerfilCuidadorDTORequest.class)))
                .thenReturn(crearResponse(List.of(Titulacion.AUXILIAR_ENFERMERIA), "Otra formación"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<PerfilCuidadorDTORequest> captor = ArgumentCaptor.forClass(PerfilCuidadorDTORequest.class);
        verify(service).storeEntity(captor.capture());
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(captor.getValue().titulaciones(), contains(Titulacion.AUXILIAR_ENFERMERIA));
        assertThat(captor.getValue().otrasTitulaciones(), is(equalTo("Otra formación")));
    }

    @Test
    void testStoreConUnaTitulacionQueNoExisteDevuelve400() throws Exception {
        String json = "{\"especialidad\":\"Geriatría\",\"anosExperiencia\":4,\"tarifaHora\":18.00,\"bio\":\"Bio\","
                + "\"tieneVehiculo\":true,\"disponibleAhora\":true,\"usuarioId\":2,\"titulaciones\":[\"MEDICO\"]}";

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(PerfilCuidadorDTORequest.class));
    }

    @Test
    void testStoreConOtrasTitulacionesDemasiadoLargasDevuelve400() throws Exception {
        PerfilCuidadorDTORequest dto = crearRequest(null, "a".repeat(201));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(PerfilCuidadorDTORequest.class));
    }

    @Test
    void testUpdatePasaLasTitulacionesAlServicio() throws Exception {
        PerfilCuidadorDTORequest dto = crearRequest(List.of(Titulacion.ATENCION_SOCIOSANITARIA), null);
        when(service.update(eq(1L), any(PerfilCuidadorDTORequest.class)))
                .thenReturn(crearResponse(List.of(Titulacion.ATENCION_SOCIOSANITARIA), null));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/cuidadores/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<PerfilCuidadorDTORequest> captor = ArgumentCaptor.forClass(PerfilCuidadorDTORequest.class);
        verify(service).update(eq(1L), captor.capture());
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(captor.getValue().titulaciones(), contains(Titulacion.ATENCION_SOCIOSANITARIA));
        assertThat(response.getContentAsString(), containsString("\"titulaciones\":[\"ATENCION_SOCIOSANITARIA\"]"));
    }
}
