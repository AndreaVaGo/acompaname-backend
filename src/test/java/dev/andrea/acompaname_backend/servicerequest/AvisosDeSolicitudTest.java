package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import dev.andrea.acompaname_backend.user.UsuarioEntity;

@ExtendWith(MockitoExtension.class)
public class AvisosDeSolicitudTest {

    private static final String URL_FRONT = "http://localhost:5173";

    @Mock
    private ServicioDeCorreo correo;

    private AvisosDeSolicitud avisos;
    private SolicitudEntity solicitud;

    @BeforeEach
    void setup() {
        avisos = new AvisosDeSolicitud(correo, URL_FRONT);

        UsuarioEntity ana = UsuarioEntity.builder().id(1L).nombre("Ana").email("ana@test.com").build();
        UsuarioEntity pepe = UsuarioEntity.builder().id(2L).nombre("Pepe").email("pepe@test.com").build();
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity();
        perfilPepe.setId(1L);
        perfilPepe.setUsuario(pepe);

        solicitud = SolicitudEntity.builder()
                .id(10L)
                .tipoCuidado("Acompañamiento")
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .estado(EstadoSolicitud.PENDIENTE)
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

    // ---------- solicitud nueva ----------

    @Test
    void testSolicitudNuevaVaAlCuidadorConElNombreDeLaFamilia() {
        avisos.avisarSolicitudNueva(solicitud);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("pepe@test.com")));
        assertThat(enviado[1], is(equalTo("Tienes una solicitud nueva en Acompáñame")));
        assertThat(enviado[2], containsString("Hola Pepe"));
        assertThat(enviado[2], containsString("Ana te ha enviado una solicitud de cuidado (Acompañamiento)"));
    }

    @Test
    void testSolicitudNuevaLlevaElEnlaceALasSolicitudesRecibidas() {
        avisos.avisarSolicitudNueva(solicitud);

        assertThat(correoEnviado()[2], containsString(URL_FRONT + "/solicitudes-cuidador"));
    }

    @Test
    void testUnSoloDiaSeEscribeConPara() {
        avisos.avisarSolicitudNueva(solicitud);

        assertThat(correoEnviado()[2], containsString("para el 15/12/2026"));
    }

    @Test
    void testSiLaFechaFinEsLaMismaTambienEsUnSoloDia() {
        solicitud.setFechaFin(LocalDate.of(2026, 12, 15));

        avisos.avisarSolicitudNueva(solicitud);

        assertThat(correoEnviado()[2], containsString("para el 15/12/2026"));
    }

    @Test
    void testVariosDiasSeEscribenConDelAl() {
        solicitud.setFechaFin(LocalDate.of(2026, 12, 18));

        avisos.avisarSolicitudNueva(solicitud);

        assertThat(correoEnviado()[2], containsString("del 15/12/2026 al 18/12/2026"));
    }

    @Test
    void testSinTipoDeCuidadoNoSalenParentesisVacios() {
        solicitud.setTipoCuidado(null);

        avisos.avisarSolicitudNueva(solicitud);

        String texto = correoEnviado()[2];
        assertThat(texto, containsString("solicitud de cuidado para el"));
        assertThat(texto, is(not(containsString("()"))));
    }

    @Test
    void testElCorreoNoLlevaLaDireccionNiLasNotasDelPaciente() {
        solicitud.setDireccion("Calle Privada 5, 2ºB");
        solicitud.setNotas("Toma medicación a las 9");
        solicitud.setNombrePaciente("Manuel");

        avisos.avisarSolicitudNueva(solicitud);

        String texto = correoEnviado()[2];
        assertThat(texto, is(not(containsString("Calle Privada"))));
        assertThat(texto, is(not(containsString("medicación"))));
        assertThat(texto, is(not(containsString("Manuel"))));
    }

    // ---------- solicitud aceptada ----------

    @Test
    void testSolicitudAceptadaVaALaFamiliaConElNombreDelCuidador() {
        avisos.avisarSolicitudAceptada(solicitud);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("ana@test.com")));
        assertThat(enviado[1], is(equalTo("Tu solicitud ha sido aceptada")));
        assertThat(enviado[2], containsString("Hola Ana"));
        assertThat(enviado[2], containsString("Pepe ha aceptado tu solicitud de cuidado (Acompañamiento)"));
        assertThat(enviado[2], containsString("para el 15/12/2026"));
    }

    @Test
    void testSolicitudAceptadaLlevaElEnlaceALasSolicitudesDeLaFamilia() {
        avisos.avisarSolicitudAceptada(solicitud);

        assertThat(correoEnviado()[2], containsString(URL_FRONT + "/solicitudes"));
    }

    // ---------- solicitud rechazada ----------

    @Test
    void testSolicitudRechazadaVaALaFamiliaYInvitaABuscarOtraPersona() {
        avisos.avisarSolicitudRechazada(solicitud);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("ana@test.com")));
        assertThat(enviado[1], is(equalTo("Tu solicitud no ha podido ser aceptada")));
        assertThat(enviado[2], containsString("Pepe no puede atender tu solicitud"));
        assertThat(enviado[2], containsString(URL_FRONT + "/buscar"));
    }

    // ---------- si algo falla, no se rompe nada ----------

    @Test
    void testSiFallaElEnvioNoSePropagaElError() {
        doThrow(new CorreoException("fallo", new RuntimeException())).when(correo).enviar(anyString(), anyString(),
                anyString());

        assertDoesNotThrow(() -> avisos.avisarSolicitudNueva(solicitud));
        assertDoesNotThrow(() -> avisos.avisarSolicitudAceptada(solicitud));
        assertDoesNotThrow(() -> avisos.avisarSolicitudRechazada(solicitud));
    }

    @Test
    void testSiFaltanDatosDeLaSolicitudNoSePropagaElErrorNiSeEnvia() {
        SolicitudEntity incompleta = new SolicitudEntity();
        incompleta.setId(99L);

        assertDoesNotThrow(() -> avisos.avisarSolicitudNueva(incompleta));
        assertDoesNotThrow(() -> avisos.avisarSolicitudAceptada(incompleta));
        assertDoesNotThrow(() -> avisos.avisarSolicitudRechazada(incompleta));

        verifyNoInteractions(correo);
    }
}
