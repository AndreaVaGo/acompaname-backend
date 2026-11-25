package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// La clave de desarrollo es pública: en un servidor real la aplicación no debe arrancar con ella
public class GuardiaDeClaveJwtTest {

    private static final String CLAVE_PROPIA = "una-clave-propia-larga-y-secreta-de-produccion";

    @Test
    void testClaveDeDesarrolloConOrigenLocalArranca() {
        assertDoesNotThrow(() -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                new String[] { "http://localhost:5173" }));
    }

    @Test
    void testClaveDeDesarrolloConVariosOrigenesLocalesArranca() {
        assertDoesNotThrow(() -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                new String[] { "http://localhost:5173", "http://127.0.0.1:5174", "http://[::1]:5173" }));
    }

    @Test
    void testClaveDeDesarrolloConUnOrigenRealNoArranca() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                        new String[] { "https://acompaname.es" }));

        assertThat(error.getMessage(), containsString("https://acompaname.es"));
        assertThat(error.getMessage(), containsString("JWT_SECRET"));
    }

    @Test
    void testBastaUnOrigenRealEntreVariosParaQueNoArranque() {
        assertThrows(IllegalStateException.class,
                () -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                        new String[] { "http://localhost:5173", "https://acompaname.es" }));
    }

    @Test
    void testUnOrigenQueNoSeEntiendeNoSeDaPorLocal() {
        assertThrows(IllegalStateException.class,
                () -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                        new String[] { "esto no es una direccion" }));
    }

    @Test
    void testUnDominioQueEmpiezaPorLocalhostPeroNoLoEsNoSeDaPorLocal() {
        assertThrows(IllegalStateException.class,
                () -> GuardiaDeClaveJwt.comprobar(GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO,
                        new String[] { "http://localhost.evil.com" }));
    }

    @Test
    void testUnaClavePropiaArrancaConCualquierOrigen() {
        assertDoesNotThrow(() -> GuardiaDeClaveJwt.comprobar(CLAVE_PROPIA,
                new String[] { "https://acompaname.es" }));
    }

    @Test
    void testLaClaveDeDesarrolloDelCodigoEsLaMismaQueLaDeApplicationProperties() throws IOException {
        Properties propiedades = new Properties();
        try (InputStream entrada = new ClassPathResource("application.properties").getInputStream()) {
            propiedades.load(entrada);
        }

        // Si alguien cambia la clave por defecto en un sitio y no en el otro, la guardia dejaría de proteger
        assertThat(propiedades.getProperty("jwt.secret"),
                is(equalTo("${JWT_SECRET:" + GuardiaDeClaveJwt.CLAVE_DE_DESARROLLO + "}")));
    }
}
