package dev.andrea.acompaname_backend.geolocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// La aplicación completa: la búsqueda por cercanía (pública) se corta con 429 al pasar el límite por IP.
// Se baja a 2 búsquedas por ventana de 60 s; con la geolocalización apagada no se sale a internet.
@SpringBootTest(properties = { "rate-limit.cerca-max-requests=2", "rate-limit.window-seconds=60",
        "geocoding.enabled=false" })
@AutoConfigureMockMvc
public class CercaniaLimiteIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private MockMvc mockMvc;

    private int buscar() throws Exception {
        return mockMvc.perform(get("/api/v1/cuidadores/cerca?lugar=Oviedo")).andReturn().getResponse().getStatus();
    }

    @Test
    void testLaTerceraBusquedaSeguidaDa429() throws Exception {
        assertEquals(200, buscar());
        assertEquals(200, buscar());

        assertEquals(429, buscar());
    }
}
