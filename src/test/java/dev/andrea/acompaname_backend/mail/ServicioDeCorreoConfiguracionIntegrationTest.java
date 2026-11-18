package dev.andrea.acompaname_backend.mail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Con la aplicación completa y sin tocar nada (mail.enabled sin definir), se usa el servicio que escribe en el log
@SpringBootTest
public class ServicioDeCorreoConfiguracionIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private ServicioDeCorreo servicio;

    @Test
    void testPorDefectoSeUsaElServicioQueEscribeEnElLog() {
        assertThat(servicio, instanceOf(ServicioDeCorreoEnLog.class));
    }
}
