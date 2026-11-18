package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

// Tests del endpoint que dice qué cuidadores están ocupados en unas fechas
@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerOcupadosTest {
        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private SolicitudService service;
        @Autowired
        ObjectMapper mapper;

        @Test
        void testCuidadoresOcupadosConInicioYFin() throws Exception {
                List<Long> ocupados = List.of(1L, 3L);
                when(service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), null, null))
                                .thenReturn(ocupados);

                MockHttpServletResponse response = mockMvc
                                .perform(get("/api/v1/solicitudes/cuidadores-ocupados")
                                                .param("inicio", "2026-09-10")
                                                .param("fin", "2026-09-12"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response.getContentAsString(), is(equalTo(mapper.writeValueAsString(ocupados))));
        }

        @Test
        void testCuidadoresOcupadosSoloConInicioBuscaUnDia() throws Exception {
                when(service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null, null, null)).thenReturn(List.of(2L));

                mockMvc.perform(get("/api/v1/solicitudes/cuidadores-ocupados")
                                .param("inicio", "2026-09-10"))
                                .andExpect(status().isOk());

                verify(service).getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null, null, null);
        }

        @Test
        void testCuidadoresOcupadosConHoras() throws Exception {
                when(service.getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null, LocalTime.of(15, 0),
                                LocalTime.of(18, 0))).thenReturn(List.of(4L));

                MockHttpServletResponse response = mockMvc
                                .perform(get("/api/v1/solicitudes/cuidadores-ocupados")
                                                .param("inicio", "2026-09-10")
                                                .param("horaInicio", "15:00")
                                                .param("horaFin", "18:00"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getContentAsString(), is(equalTo("[4]")));
                verify(service).getCuidadoresOcupados(LocalDate.of(2026, 9, 10), null, LocalTime.of(15, 0),
                                LocalTime.of(18, 0));
        }

        @Test
        void testCuidadoresOcupadosConUnaHoraQueNoExisteDevuelve400() throws Exception {
                mockMvc.perform(get("/api/v1/solicitudes/cuidadores-ocupados")
                                .param("inicio", "2026-09-10")
                                .param("horaInicio", "25:99"))
                                .andExpect(status().isBadRequest());
        }
}