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
        // 31 caracteres: 26 letras y 5 números
        String clave = "abcdefghijklmnopqrstuvwxyz12345";

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
        // 32 caracteres: 26 letras y 6 números
        String clave = "abcdefghijklmnopqrstuvwxyz123456";

        assertDoesNotThrow(() -> new JwtConfiguration(clave));
    }
}