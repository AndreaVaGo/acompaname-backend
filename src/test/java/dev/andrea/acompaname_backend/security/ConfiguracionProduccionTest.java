package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// Comprueba que la configuración de producción no trae contraseñas por defecto.
public class ConfiguracionProduccionTest {

    private Properties cargarPropiedadesDeProduccion() throws IOException {
        Properties propiedades = new Properties();
        InputStream entrada = new ClassPathResource("application-prod.properties").getInputStream();
        propiedades.load(entrada);
        entrada.close();
        return propiedades;
    }

    @Test
    void testElSecretoDelJwtNoTieneValorPorDefecto() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("jwt.secret"), is(equalTo("${JWT_SECRET}")));
    }

    @Test
    void testLaContrasenaDeLaBaseDeDatosNoTieneValorPorDefecto() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("spring.datasource.password"), is(equalTo("${DB_PASSWORD}")));
    }

    @Test
    void testElUsuarioDeLaBaseDeDatosNoTieneValorPorDefecto() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("spring.datasource.username"), is(equalTo("${DB_USER}")));
    }

    @Test
    void testNingunValorDeProduccionUsaLasClavesDeDesarrollo() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        for (String nombre : propiedades.stringPropertyNames()) {
            String valor = propiedades.getProperty(nombre);
            assertThat(valor.contains("acompaname_pass"), is(false));
            assertThat(valor.contains("clave-solo-para-desarrollo"), is(false));
        }
    }

    @Test
    void testElArchivoDeProduccionExiste() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("jwt.secret"), is(not(nullValue())));
    }

    @Test
    void testElCorreoDeProduccionSeEnviaDeVerdadYNoTieneValoresPorDefecto() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("mail.enabled"), is(equalTo("true")));
        assertThat(propiedades.getProperty("spring.mail.host"), is(equalTo("${MAIL_HOST}")));
        assertThat(propiedades.getProperty("spring.mail.password"), is(equalTo("${MAIL_PASSWORD}")));
        assertThat(propiedades.getProperty("mail.from"), is(equalTo("${MAIL_FROM}")));
    }

    @Test
    void testLaDireccionDelFrontParaLosEnlacesNoTieneValorPorDefecto() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("app.frontend-url"), is(equalTo("${FRONTEND_URL}")));
    }
}
