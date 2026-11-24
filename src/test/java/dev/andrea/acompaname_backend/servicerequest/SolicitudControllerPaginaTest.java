package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.generics.PaginaDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;

// Tests del endpoint que devuelve las solicitudes del usuario por páginas
@WebMvcTest(controllers = SolicitudController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SolicitudControllerPaginaTest {
        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private SolicitudService service;

        @Test
        void testSinParametrosPideLaPrimeraPaginaDeVeinte() throws Exception {
                when(service.getMisSolicitudesPaginadas(0, 20))
                                .thenReturn(new PaginaDTOResponse<SolicitudDTOResponse>(List.of(), 0, 20, 0L, 0));

                MockHttpServletResponse response = mockMvc.perform(get("/api/v1/solicitudes/mis-solicitudes/pagina"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getContentAsString(), containsString("\"contenido\":[]"));
                assertThat(response.getContentAsString(), containsString("\"totalElementos\":0"));
                verify(service).getMisSolicitudesPaginadas(0, 20);
        }

        @Test
        void testLaPaginaYElTamanoPedidosLlegansAlServicio() throws Exception {
                when(service.getMisSolicitudesPaginadas(2, 5))
                                .thenReturn(new PaginaDTOResponse<SolicitudDTOResponse>(List.of(), 2, 5, 11L, 3));

                MockHttpServletResponse response = mockMvc
                                .perform(get("/api/v1/solicitudes/mis-solicitudes/pagina")
                                                .param("page", "2")
                                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse();

                assertThat(response.getStatus(), is(equalTo(200)));
                assertThat(response.getContentAsString(), containsString("\"pagina\":2"));
                assertThat(response.getContentAsString(), containsString("\"totalPaginas\":3"));
                verify(service).getMisSolicitudesPaginadas(2, 5);
        }

        @Test
        void testUnaPaginaQueNoEsUnNumeroDa400() throws Exception {
                mockMvc.perform(get("/api/v1/solicitudes/mis-solicitudes/pagina").param("page", "abc"))
                                .andExpect(status().isBadRequest());
        }
}
