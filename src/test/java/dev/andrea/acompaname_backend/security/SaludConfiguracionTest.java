package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// Si alguien expone más endpoints de actuator o activa los detalles, este test avisa
public class SaludConfiguracionTest {

    private Properties cargar(String fichero) throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = new ClassPathResource(fichero).getInputStream()) {
            propiedades.load(entrada);
        }
        return propiedades;
    }

    @Test
    void testSoloSeExponeLaSalud() throws IOException {
        Properties propiedades = cargar("application.properties");

        assertThat(propiedades.getProperty("management.endpoints.web.exposure.include"), is(equalTo("health")));
    }

    @Test
    void testLaSaludNuncaEnseñaDetalles() throws IOException {
        Properties propiedades = cargar("application.properties");

        assertThat(propiedades.getProperty("management.endpoint.health.show-details"), is(equalTo("never")));
    }

    @Test
    void testProduccionNoCambiaLoQueSeExpone() throws IOException {
        Properties produccion = cargar("application-prod.properties");

        assertThat(produccion.getProperty("management.endpoints.web.exposure.include"), is(equalTo(null)));
        assertThat(produccion.getProperty("management.endpoint.health.show-details"), is(equalTo(null)));
    }
}
