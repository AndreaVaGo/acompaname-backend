package dev.andrea.acompaname_backend.mail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

// Tests del envío por SMTP, sin servidor de correo: se vigila lo que se le manda al JavaMailSender
@ExtendWith(MockitoExtension.class)
public class ServicioDeCorreoSmtpTest {

    @Mock
    private JavaMailSender enviador;

    private ServicioDeCorreoSmtp servicio;

    @BeforeEach
    void setup() {
        servicio = new ServicioDeCorreoSmtp(enviador, "no-reply@acompaname.com");
    }

    @Test
    void testEnviaElCorreoConElRemitenteElDestinoElAsuntoYElTexto() {
        servicio.enviar("ana@test.com", "Hola", "Texto del correo");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(captor.capture());
        SimpleMailMessage enviado = captor.getValue();
        assertThat(enviado.getFrom(), is(equalTo("no-reply@acompaname.com")));
        assertThat(enviado.getTo()[0], is(equalTo("ana@test.com")));
        assertThat(enviado.getSubject(), is(equalTo("Hola")));
        assertThat(enviado.getText(), is(equalTo("Texto del correo")));
    }

    @Test
    void testSoloSeEnviaAUnaPersona() {
        servicio.enviar("ana@test.com", "Hola", "Texto");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviador).send(captor.capture());
        assertThat(captor.getValue().getTo().length, is(equalTo(1)));
    }

    @Test
    void testSiFallaElEnvioSeLanzaCorreoException() {
        doThrow(new MailSendException("el servidor no responde")).when(enviador).send(any(SimpleMailMessage.class));

        CorreoException error = assertThrows(CorreoException.class,
                () -> servicio.enviar("ana@test.com", "Hola", "Texto"));

        assertThat(error.getMessage(), is(equalTo("No se pudo enviar el correo")));
        assertThat(error.getCause() instanceof MailSendException, is(true));
    }
}
