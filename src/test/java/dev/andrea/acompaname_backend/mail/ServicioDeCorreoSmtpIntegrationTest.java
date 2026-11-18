package dev.andrea.acompaname_backend.mail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import dev.andrea.acompaname_backend.support.ContenedorMySqlAplicacion;

// Con mail.enabled=true se activa el servicio que envía por SMTP (no se envía nada, solo se comprueba qué servicio hay)
@SpringBootTest(properties = "mail.enabled=true")
public class ServicioDeCorreoSmtpIntegrationTest extends ContenedorMySqlAplicacion {

    @Autowired
    private ServicioDeCorreo servicio;

    @Test
    void testConMailEnabledSeUsaElServicioSmtp() {
        assertThat(servicio, instanceOf(ServicioDeCorreoSmtp.class));
    }
}
