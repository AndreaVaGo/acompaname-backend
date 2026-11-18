package dev.andrea.acompaname_backend.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

// El servicio de desarrollo solo escribe en el log: lo importante es que nunca falle
public class ServicioDeCorreoEnLogTest {

    @Test
    void testEnviarNoLanzaNingunError() {
        ServicioDeCorreoEnLog servicio = new ServicioDeCorreoEnLog();

        assertDoesNotThrow(() -> servicio.enviar("ana@test.com", "Hola", "Texto del correo"));
    }

    @Test
    void testEnviarFuncionaConTextosVacios() {
        ServicioDeCorreoEnLog servicio = new ServicioDeCorreoEnLog();

        assertDoesNotThrow(() -> servicio.enviar("ana@test.com", "", ""));
    }
}
