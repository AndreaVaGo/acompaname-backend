package dev.andrea.acompaname_backend.valoracion;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ValoracionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ValoracionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ValoracionService service;
    @Autowired
    ObjectMapper mapper;

    private SolicitudEntity crearSolicitudMock() {
        RoleEntity rolFamilia = new RoleEntity();
        rolFamilia.setId(1L);
        rolFamilia.setName("FAMILIA");
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rolFamilia));

        RoleEntity rolCuidador = new RoleEntity();
        rolCuidador.setId(2L);
        rolCuidador.setName("CUIDADOR");
        UsuarioEntity usuarioCuidador = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rolCuidador));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, usuarioCuidador);

        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, familia, cuidador);
    }

    @Test
    void testIndex() throws Exception {
        ValoracionEntity valoracion = new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11),
                crearSolicitudMock());
        List<ValoracionEntity> valoraciones = new ArrayList<>();
        valoraciones.add(valoracion);
        String json = mapper.writeValueAsString(valoraciones);
        when(service.getEntities()).thenReturn(valoraciones);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/valoraciones"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Muy buena atención"));
    }

    @Test
    void testGetById() throws Exception {
        ValoracionEntity valoracion = new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11),
                crearSolicitudMock());
        String json = mapper.writeValueAsString(valoracion);
        when(service.getById(1L)).thenReturn(valoracion);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/valoraciones/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Muy buena atención"));
    }

    @Test
    void testStore() throws Exception {
        ValoracionDTORequest dto = new ValoracionDTORequest("Excelente trato", 5, LocalDate.of(2026, 9, 12), 1L);
        ValoracionDTOResponse dtoResponse = new ValoracionDTOResponse(1L, "Excelente trato", 5,
                LocalDate.of(2026, 9, 12), 1L);
        String json = mapper.writeValueAsString(dtoResponse);
        when(service.storeEntity(Mockito.any(ValoracionDTORequest.class))).thenReturn(dtoResponse);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/valoraciones")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(response.getContentAsString(), containsString("Excelente trato"));
    }

    @Test
    void testDelete() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/valoraciones/1"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus(), is(equalTo(204)));
        Mockito.verify(service).deleteById(1L);
    }
}