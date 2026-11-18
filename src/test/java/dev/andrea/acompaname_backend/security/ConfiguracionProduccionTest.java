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

    @Test
    void testDetrasDelProxyTomcatLeeLaIpRealDelCliente() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("server.forward-headers-strategy"), is(equalTo("native")));
    }

    @Test
    void testNoSeAmpliaLaListaDeProxiesDeConfianza() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        // Si se pusiera una lista propia (por ejemplo ".*"), cualquiera podría falsear su IP
        assertThat(propiedades.getProperty("server.tomcat.remoteip.internal-proxies"), is(nullValue()));
    }

    // ---- Endurecimiento de producción (ACOM-211) ----

    @Test
    void testLaBaseDeDatosSoloSeValidaYNuncaSeModificaSola() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        // validate por defecto; solo se puede cambiar a mano con la variable JPA_DDL_AUTO
        assertThat(propiedades.getProperty("spring.jpa.hibernate.ddl-auto"), is(equalTo("${JPA_DDL_AUTO:validate}")));
    }

    @Test
    void testNoSeEscribenLasConsultasSqlEnElLog() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("spring.jpa.show-sql"), is(equalTo("false")));
        assertThat(propiedades.getProperty("logging.level.org.hibernate.SQL"), is(equalTo("WARN")));
    }

    @Test
    void testLosErroresNoEnsenanMensajesNiTrazasNiLaExcepcion() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("server.error.include-message"), is(equalTo("never")));
        assertThat(propiedades.getProperty("server.error.include-stacktrace"), is(equalTo("never")));
        assertThat(propiedades.getProperty("server.error.include-binding-errors"), is(equalTo("never")));
        assertThat(propiedades.getProperty("server.error.include-exception"), is(equalTo("false")));
        assertThat(propiedades.getProperty("server.error.whitelabel.enabled"), is(equalTo("false")));
    }

    @Test
    void testElNivelDeLogEsInfo() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("logging.level.root"), is(equalTo("INFO")));
    }

    @Test
    void testDockerComposeEstaDesactivadoEnProduccion() throws IOException {
        Properties propiedades = cargarPropiedadesDeProduccion();

        assertThat(propiedades.getProperty("spring.docker.compose.enabled"), is(equalTo("false")));
    }
}
