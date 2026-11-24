package dev.andrea.acompaname_backend.fichaje;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDateTime;
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

import dev.andrea.acompaname_backend.fichaje.dtos.FichajeConfiguracionDTOResponse;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTORequest;
import dev.andrea.acompaname_backend.fichaje.dtos.FichajeDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionEstadoInvalido;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;

@WebMvcTest(controllers = FichajeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FichajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FichajeService service;

    private FichajeDTOResponse inicio() {
        return new FichajeDTOResponse(1L, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 10, 2),
                ResultadoUbicacion.EN_EL_LUGAR);
    }

    private MockHttpServletResponse enviar(String ruta, String cuerpo) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse();
    }

    @Test
    void testInicioDevuelve201ConElFichaje() throws Exception {
        when(service.empezar(5L, new FichajeDTORequest(43.36, -5.85))).thenReturn(inicio());

        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/inicio",
                "{\"latitud\":43.36,\"longitud\":-5.85}");

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("EN_EL_LUGAR"));
        assertThat(response.getContentAsString(), containsString("INICIO"));
    }

    @Test
    void testFinDevuelve201() throws Exception {
        when(service.terminar(5L, new FichajeDTORequest(43.36, -5.85)))
                .thenReturn(new FichajeDTOResponse(2L, TipoFichaje.FIN, LocalDateTime.of(2026, 11, 24, 14, 0),
                        ResultadoUbicacion.FUERA_DEL_LUGAR));

        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/fin",
                "{\"latitud\":43.36,\"longitud\":-5.85}");

        assertThat(response.getStatus(), is(equalTo(201)));
        assertThat(response.getContentAsString(), containsString("FUERA_DEL_LUGAR"));
    }

    @Test
    void testSinCuerpoSeFichaSinUbicacion() throws Exception {
        when(service.empezar(5L, new FichajeDTORequest(null, null))).thenReturn(inicio());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/fichajes/5/inicio")).andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(201)));
        verify(service).empezar(5L, new FichajeDTORequest(null, null));
    }

    @Test
    void testConLlaveVaciaTambienSeFichaSinUbicacion() throws Exception {
        when(service.empezar(5L, new FichajeDTORequest(null, null))).thenReturn(inicio());

        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/inicio", "{}");

        assertThat(response.getStatus(), is(equalTo(201)));
    }

    @Test
    void testLatitudFueraDeRangoDa400YNoFicha() throws Exception {
        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/inicio",
                "{\"latitud\":100,\"longitud\":-5.85}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La latitud tiene que estar entre -90 y 90"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testLongitudFueraDeRangoDa400() throws Exception {
        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/fin",
                "{\"latitud\":43.36,\"longitud\":200}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La longitud tiene que estar entre -180 y 180"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testUnaSolaCoordenadaDa400() throws Exception {
        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/inicio", "{\"latitud\":43.36}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("La latitud y la longitud se mandan juntas"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void testIndexDevuelveLosFichajes() throws Exception {
        when(service.listar(5L)).thenReturn(List.of(inicio()));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/fichajes/5")).andReturn().getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("EN_EL_LUGAR"));
    }

    @Test
    void testConfiguracionDiceSiHayQuePedirLaUbicacion() throws Exception {
        when(service.configuracion()).thenReturn(new FichajeConfiguracionDTOResponse(true));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/fichajes/configuracion")).andReturn()
                .getResponse();

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response.getContentAsString(), containsString("\"verificarLugar\":true"));
    }

    @Test
    void testLosErroresDelServicioSeConviertenEnSuCodigo() throws Exception {
        when(service.empezar(any(), any())).thenThrow(new SolicitudExceptionAccesoDenegado("no"));
        when(service.terminar(any(), any())).thenThrow(new SolicitudExceptionEstadoInvalido("estado"));
        when(service.listar(7L)).thenThrow(new SolicitudExceptionNotFound("no existe"));

        assertThat(enviar("/api/v1/fichajes/5/inicio", "{}").getStatus(), is(equalTo(403)));
        assertThat(enviar("/api/v1/fichajes/5/fin", "{}").getStatus(), is(equalTo(409)));
        assertThat(mockMvc.perform(get("/api/v1/fichajes/7")).andReturn().getResponse().getStatus(),
                is(equalTo(404)));
    }

    @Test
    void testEmpezarAntesDeTiempoDa400() throws Exception {
        when(service.empezar(any(), any()))
                .thenThrow(new SolicitudExceptionFechaInvalida("Todavía no es el día del servicio"));

        MockHttpServletResponse response = enviar("/api/v1/fichajes/5/inicio", "{}");

        assertThat(response.getStatus(), is(equalTo(400)));
        assertThat(response.getContentAsString(), containsString("Todavía no es el día del servicio"));
    }
}
