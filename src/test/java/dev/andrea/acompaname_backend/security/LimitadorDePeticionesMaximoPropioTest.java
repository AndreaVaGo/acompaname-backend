package dev.andrea.acompaname_backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// Algunas rutas necesitan un máximo distinto al general (por ejemplo, la búsqueda por cercanía)
public class LimitadorDePeticionesMaximoPropioTest {

    @Test
    void testConUnMaximoPropioSeCortaCuandoSeLlegaAEse() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(5, 60);
        limitador.comprobar("cerca:1.1.1.1", 2);
        limitador.comprobar("cerca:1.1.1.1", 2);

        assertThrows(DemasiadasPeticionesException.class, () -> limitador.comprobar("cerca:1.1.1.1", 2));
    }

    @Test
    void testUnMaximoPropioMayorQueElGeneralPermiteMasPeticiones() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(1, 60);

        for (int i = 0; i < 10; i++) {
            limitador.comprobar("cerca:1.1.1.1", 10);
        }

        assertThrows(DemasiadasPeticionesException.class, () -> limitador.comprobar("cerca:1.1.1.1", 10));
    }

    @Test
    void testSinMaximoPropioSigueUsandoElGeneral() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(2, 60);
        limitador.comprobar("login:1.1.1.1");
        limitador.comprobar("login:1.1.1.1");

        assertThrows(DemasiadasPeticionesException.class, () -> limitador.comprobar("login:1.1.1.1"));
    }

    @Test
    void testLasClavesDeDistintasRutasNoSeMezclan() {
        LimitadorDePeticiones limitador = new LimitadorDePeticiones(1, 60);
        limitador.comprobar("login:1.1.1.1");

        assertDoesNotThrow(() -> limitador.comprobar("cerca:1.1.1.1", 1));
    }
}
