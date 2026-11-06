package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// La clave con la que se firman los tokens tiene que ser larga: si no, la aplicación no arranca
public class JwtConfigurationTest {

    @Test
    void testUnaClaveDeMenosDe32CaracteresHaceQueNoArranque() {
        String clave = "a".repeat(31);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new JwtConfiguration(clave));

        assertThat(error.getMessage(), containsString("32"));
    }

    @Test
    void testUnaClaveVaciaHaceQueNoArranque() {
        assertThrows(IllegalStateException.class, () -> new JwtConfiguration(""));
    }

    @Test
    void testUnaClaveDeExactamente32CaracteresEsValida() {
        assertDoesNotThrow(() -> new JwtConfiguration("a".repeat(32)));
    }
}