package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
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
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PerfilCuidadorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilCuidadorControllerGruposEdadTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PerfilCuidadorService service;
    @Autowired
    ObjectMapper mapper;

    // Petición con los grupos de edad que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<GrupoEdad> grupos) {
        return new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L, null,
                null, null, null, null, grupos);
    }

    // Respuesta con los grupos de edad que le pasemos
    private PerfilCuidadorDTOResponse crearResponse(List<GrupoEdad> grupos) {
        return new PerfilCuidadorDTOResponse(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L,
                "Pepe", null, null, 0L, "ambos", new ArrayList<>(), null, null, grupos);
    }

    @Test
    void testGetByIdDevuelveLosGruposDeEdad() throws Exception {
        when(service.getById(1L)).thenReturn(crearResponse(List.of(GrupoEdad.BEBES, GrupoEdad.NINOS)));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/1"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"gruposEdad\":[\"BEBES\",\"NINOS\"]"));
    }

    @Test
    void testStorePasaLosGruposDeEdadAlServicio() throws Exception {
        PerfilCuidadorDTORequest dto = crearRequest(List.of(GrupoEdad.MAYORES, GrupoEdad.DISCAPACIDAD_DEPENDENCIA));
        when(service.storeEntity(any(PerfilCuidadorDTORequest.class)))
                .thenReturn(crearResponse(List.of(GrupoEdad.MAYORES, GrupoEdad.DISCAPACIDAD_DEPENDENCIA)));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<PerfilCuidadorDTORequest> captor = ArgumentCaptor.forClass(PerfilCuidadorDTORequest.class);
        verify(service).storeEntity(captor.capture());
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(captor.getValue().gruposEdad(),
                contains(GrupoEdad.MAYORES, GrupoEdad.DISCAPACIDAD_DEPENDENCIA));
    }

    @Test
    void testStoreConUnGrupoQueNoExisteDevuelve400() throws Exception {
        String json = "{\"especialidad\":\"Geriatría\",\"anosExperiencia\":4,\"tarifaHora\":18.00,\"bio\":\"Bio\","
                + "\"tieneVehiculo\":true,\"disponibleAhora\":true,\"usuarioId\":2,\"gruposEdad\":[\"VIEJOS\"]}";

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(400)));
        verify(service, never()).storeEntity(any(PerfilCuidadorDTORequest.class));
    }

    @Test
    void testUpdatePasaLosGruposDeEdadAlServicio() throws Exception {
        PerfilCuidadorDTORequest dto = crearRequest(List.of(GrupoEdad.ADOLESCENTES));
        when(service.update(eq(1L), any(PerfilCuidadorDTORequest.class)))
                .thenReturn(crearResponse(List.of(GrupoEdad.ADOLESCENTES)));

        MockHttpServletResponse response = mockMvc.perform(put("/api/v1/cuidadores/1")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();

        ArgumentCaptor<PerfilCuidadorDTORequest> captor = ArgumentCaptor.forClass(PerfilCuidadorDTORequest.class);
        verify(service).update(eq(1L), captor.capture());
        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(captor.getValue().gruposEdad(), contains(GrupoEdad.ADOLESCENTES));
        assertThat(response.getContentAsString(), containsString("\"gruposEdad\":[\"ADOLESCENTES\"]"));
    }
}