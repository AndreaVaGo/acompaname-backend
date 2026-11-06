package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// Tests del limitador de peticiones, sin Spring: se crea con el máximo y el tiempo que queramos
public class LimitadorDePeticionesTest {

    @Test
    void testLasPeticionesDentroDelLimitePasan() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(3, 60);

        assertDoesNotThrow(() -> limitador.comprobar("login:1.1.1.1"));
        assertDoesNotThrow(() -> limitador.comprobar("login:1.1.1.1"));
        assertDoesNotThrow(() -> limitador.comprobar("login:1.1.1.1"));
    }

    @Test
    void testAlSuperarElLimiteSeLanzaElError() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(3, 60);
        limitador.comprobar("login:1.1.1.1");
        limitador.comprobar("login:1.1.1.1");
        limitador.comprobar("login:1.1.1.1");

        DemasiadasPeticionesException error = assertThrows(DemasiadasPeticionesException.class,
                () -> limitador.comprobar("login:1.1.1.1"));

        assertThat(error.getMessage(), containsString("Demasiados intentos"));
    }

    @Test
    void testCadaDireccionTieneSuPropiaCuenta() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(1, 60);
        limitador.comprobar("login:1.1.1.1");

        // Otra dirección distinta no se ve afectada
        assertDoesNotThrow(() -> limitador.comprobar("login:2.2.2.2"));
        // La primera ya ha gastado su petición
        assertThrows(DemasiadasPeticionesException.class, () -> limitador.comprobar("login:1.1.1.1"));
    }

    @Test
    void testElLoginYElRegistroCuentanPorSeparado() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(1, 60);
        limitador.comprobar("login:1.1.1.1");

        // La misma dirección puede registrarse aunque haya gastado su intento de login
        assertDoesNotThrow(() -> limitador.comprobar("registro:1.1.1.1"));
    }

    @Test
    void testDespuesDeEsperarVuelveAPasar() throws Exception {
        // Máximo 1 petición cada 1 segundo
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(1, 1);
        limitador.comprobar("login:1.1.1.1");
        assertThrows(DemasiadasPeticionesException.class, () -> limitador.comprobar("login:1.1.1.1"));

        // Se espera un poco más de un segundo
        Thread.sleep(1100);

        assertDoesNotThrow(() -> limitador.comprobar("login:1.1.1.1"));
    }
}