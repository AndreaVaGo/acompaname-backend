package dev.andrea.acompaname_backend.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.mail.CorreoException;
import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

@ExtendWith(MockitoExtension.class)
public class AvisosDeChatTest {

    private static final String URL_FRONT = "http://localhost:5173";

    @Mock
    private ServicioDeCorreo correo;

    private AvisosDeChat avisos;
    private SolicitudEntity solicitud;
    private UsuarioEntity ana;
    private UsuarioEntity pepe;

    @BeforeEach
    void setup() {
        avisos = new AvisosDeChat(correo, URL_FRONT);

        ana = UsuarioEntity.builder().id(1L).nombre("Ana").email("ana@test.com").build();
        pepe = UsuarioEntity.builder().id(2L).nombre("Pepe").email("pepe@test.com").build();
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity();
        perfilPepe.setId(1L);
        perfilPepe.setUsuario(pepe);

        solicitud = SolicitudEntity.builder()
                .id(10L)
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .estado(EstadoSolicitud.ACEPTADA)
                .familia(ana)
                .cuidador(perfilPepe)
                .build();
    }

    // Devuelve [destino, asunto, texto] del correo que se ha mandado
    private String[] correoEnviado() {
        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(destino.capture(), asunto.capture(), texto.capture());
        return new String[] { destino.getValue(), asunto.getValue(), texto.getValue() };
    }

    @Test
    void testSiEscribeLaFamiliaElAvisoVaALaPersonaCuidadora() {
        avisos.avisarMensajeNuevo(solicitud, ana);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("pepe@test.com")));
        assertThat(enviado[2], containsString("Hola Pepe"));
        assertThat(enviado[2], containsString("Ana te ha escrito"));
    }

    @Test
    void testSiEscribeLaPersonaCuidadoraElAvisoVaALaFamilia() {
        avisos.avisarMensajeNuevo(solicitud, pepe);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("ana@test.com")));
        assertThat(enviado[2], containsString("Hola Ana"));
        assertThat(enviado[2], containsString("Pepe te ha escrito"));
    }

    @Test
    void testElCorreoTieneElAsuntoYElEnlaceAlChatDeEsaSolicitud() {
        avisos.avisarMensajeNuevo(solicitud, ana);

        String[] enviado = correoEnviado();
        assertThat(enviado[1], is(equalTo("Tienes un mensaje nuevo en Acompáñame")));
        assertThat(enviado[2], containsString(URL_FRONT + "/chat/10"));
        assertThat(enviado[2], containsString("15/12/2026"));
    }

    @Test
    void testElCorreoExplicaQueNoSeAvisaDeNuevoHastaLeerlo() {
        avisos.avisarMensajeNuevo(solicitud, ana);

        assertThat(correoEnviado()[2], containsString("no te avisaremos de nuevo"));
    }

    @Test
    void testElCorreoNoIncluyeNingunDatoPrivadoComoElTelefono() {
        avisos.avisarMensajeNuevo(solicitud, ana);

        assertThat(correoEnviado()[2], not(containsString("@test.com")));
    }

    @Test
    void testSiFallaElCorreoNoSeRompeNada() {
        doThrow(new CorreoException("SMTP caído", new RuntimeException("sin conexión"))).when(correo).enviar(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> avisos.avisarMensajeNuevo(solicitud, ana));
    }
}
