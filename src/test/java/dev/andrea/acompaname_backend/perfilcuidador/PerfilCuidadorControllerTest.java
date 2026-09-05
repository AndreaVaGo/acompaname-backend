package dev.andrea.acompaname_backend.perfilcuidador;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.perfilcuidador.dtos.PerfilCuidadorDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PerfilCuidadorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilCuidadorControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PerfilCuidadorService service;
    @Autowired
    ObjectMapper mapper;

    @Test
    void testIndex() throws Exception {
        PerfilCuidadorDTOResponse perfil = new PerfilCuidadorDTOResponse(1L, "Fisioterapia", 5,
                new BigDecimal("15.00"), "Bio", true, true, 2L);
        List<PerfilCuidadorDTOResponse> perfiles = new ArrayList<>();
        perfiles.add(perfil);
        String json = mapper.writeValueAsString(perfiles);
        when(service.getEntities()).thenReturn(perfiles);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Fisioterapia"));
    }

    @Test
    void testGetById() throws Exception {
        PerfilCuidadorDTOResponse perfil = new PerfilCuidadorDTOResponse(1L, "Fisioterapia", 5,
                new BigDecimal("15.00"), "Bio", true, true, 2L);
        String json = mapper.writeValueAsString(perfil);
        when(service.getById(1L)).thenReturn(perfil);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Fisioterapia"));
    }

    @Test
    void testStore() throws Exception {
        PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, 2L);
        PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 4,
                new BigDecimal("18.00"), "Bio", true, true, 2L);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(PerfilCuidadorDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Geriatría"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/cuidadores/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }
}