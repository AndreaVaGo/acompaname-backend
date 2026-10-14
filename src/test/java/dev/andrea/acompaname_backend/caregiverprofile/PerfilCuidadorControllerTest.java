package dev.andrea.acompaname_backend.caregiverprofile;

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

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
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
                                new BigDecimal("15.00"), "Bio", true, true, 2L, "Pepe");
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
                                new BigDecimal("15.00"), "Bio", true, true, 2L, "Pepe");
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
                PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 5,
                                new BigDecimal("15.00"), "Bio", true, true, 2L, "Pepe");
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
        void testStoreAnosExperienciaNegativos() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", -1, new BigDecimal("18.00"),
                                "Bio", true, true, 2L);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(),
                                containsString("Los años de experiencia no pueden ser negativos"));
        }

        @Test
        void testStoreTarifaNegativa() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("-5.00"),
                                "Bio", true, true, 2L);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("La tarifa/hora no puede ser negativa"));
        }

        @Test
        void testDelete() throws Exception {
                MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/cuidadores/1"))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(204)));
                Mockito.verify(service).deleteById(1L);
        }

        @Test
        void testStoreBioDemasiadoLarga() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "a".repeat(1001), true, true, 2L);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("La biografía no puede superar 1000 caracteres"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testStoreEspecialidadVacia() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("La especialidad no puede estar vacío"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testStoreEspecialidadDemasiadoLarga() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("a".repeat(101), 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("La especialidad no puede superar 100 caracteres"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testStoreFotoDemasiadoLarga() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, "a".repeat(500001));

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("La foto es demasiado grande"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testStoreConTipoDeCuidadoInvalidoDevuelve400() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, null, "nocturno");

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(),
                                containsString("El tipo de cuidado debe ser hospitalario, domicilio o ambos"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testStoreConTipoDeCuidadoValido() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, null, "domicilio");
                PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 4,
                                new BigDecimal("18.00"), "Bio", true, true, 2L, "Pepe", null, null, 0L, "domicilio");
                when(service.storeEntity(Mockito.any(PerfilCuidadorDTORequest.class))).thenReturn(dtoResponse);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(201)));
                assertThat(response.getContentAsString(), containsString("domicilio"));
        }

        @Test
        void testStoreSinTipoDeCuidadoEsValido() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L);
                PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 4,
                                new BigDecimal("18.00"), "Bio", true, true, 2L, "Pepe");
                when(service.storeEntity(Mockito.any(PerfilCuidadorDTORequest.class))).thenReturn(dtoResponse);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(201)));
        }

        @Test
        void testStoreConServicios() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, null, null, List.of("movilidad", "higiene"));
                PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 4,
                                new BigDecimal("18.00"), "Bio", true, true, 2L, "Pepe", null, null, 0L, "ambos",
                                List.of("movilidad", "higiene"));
                when(service.storeEntity(Mockito.any(PerfilCuidadorDTORequest.class))).thenReturn(dtoResponse);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(201)));
                assertThat(response.getContentAsString(), containsString("movilidad"));
                assertThat(response.getContentAsString(), containsString("higiene"));
        }

        @Test
        void testGetByIdDevuelveLosServicios() throws Exception {
                PerfilCuidadorDTOResponse perfil = new PerfilCuidadorDTOResponse(1L, "Fisioterapia", 5,
                                new BigDecimal("15.00"), "Bio", true, true, 2L, "Pepe", null, null, 0L, "ambos",
                                List.of("paliativos"));
                when(service.getById(1L)).thenReturn(perfil);

                MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/1"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response.getContentAsString(), containsString("paliativos"));
        }

        @Test
        void testStoreConSexoValido() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, null, null, null, "MUJER");
                PerfilCuidadorDTOResponse dtoResponse = new PerfilCuidadorDTOResponse(1L, "Geriatría", 4,
                                new BigDecimal("18.00"), "Bio", true, true, 2L, "Pepe", null, null, 0L, "ambos",
                                List.of(), "MUJER");
                when(service.storeEntity(Mockito.any(PerfilCuidadorDTORequest.class))).thenReturn(dtoResponse);

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(201)));
                assertThat(response.getContentAsString(), containsString("MUJER"));
        }

        @Test
        void testStoreConSexoInvalidoDevuelve400() throws Exception {
                PerfilCuidadorDTORequest dto = new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"),
                                "Bio", true, true, 2L, null, null, null, "OTRO");

                MockHttpServletResponse response = mockMvc.perform(post("/api/v1/cuidadores")
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dto)))
                                .andReturn()
                                .getResponse();
                assertThat(response.getStatus(), is(equalTo(400)));
                assertThat(response.getContentAsString(), containsString("El sexo debe ser MUJER o HOMBRE"));
                Mockito.verifyNoInteractions(service);
        }

        @Test
        void testGetByIdDevuelveElSexo() throws Exception {
                PerfilCuidadorDTOResponse perfil = new PerfilCuidadorDTOResponse(1L, "Fisioterapia", 5,
                                new BigDecimal("15.00"), "Bio", true, true, 2L, "Pepe", null, null, 0L, "ambos",
                                List.of(), "HOMBRE");
                when(service.getById(1L)).thenReturn(perfil);

                MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cuidadores/1"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response.getContentAsString(), containsString("HOMBRE"));
        }
}