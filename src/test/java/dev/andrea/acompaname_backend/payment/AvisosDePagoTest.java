package dev.andrea.acompaname_backend.payment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
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

// Comprueba el correo que recibe la persona cuidadora cuando la familia paga
@ExtendWith(MockitoExtension.class)
public class AvisosDePagoTest {

    private static final String URL_FRONT = "http://localhost:5173";

    @Mock
    private ServicioDeCorreo correo;

    private AvisosDePago avisos;
    private PagoEntity pago;

    @BeforeEach
    void setup() {
        avisos = new AvisosDePago(correo, URL_FRONT);

        UsuarioEntity ana = UsuarioEntity.builder().id(1L).nombre("Ana").email("ana@test.com").build();
        UsuarioEntity pepe = UsuarioEntity.builder().id(2L).nombre("Pepe").email("pepe@test.com").build();
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity();
        perfilPepe.setId(1L);
        perfilPepe.setUsuario(pepe);

        SolicitudEntity solicitud = SolicitudEntity.builder()
                .id(10L)
                .tipoCuidado("Acompañamiento")
                .nombrePaciente("Manuel")
                .direccion("Calle Secreta 5")
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .estado(EstadoSolicitud.COMPLETADA)
                .familia(ana)
                .cuidador(perfilPepe)
                .build();

        pago = PagoEntity.builder()
                .id(5L)
                .importe(new BigDecimal("58.00"))
                .estado(EstadoPago.COMPLETADO)
                .fecha(LocalDate.of(2026, 12, 16))
                .solicitud(solicitud)
                .build();
    }

    private String[] correoEnviado() {
        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asunto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(destino.capture(), asunto.capture(), texto.capture());
        return new String[] { destino.getValue(), asunto.getValue(), texto.getValue() };
    }

    @Test
    void testElAvisoVaAlCuidador() {
        avisos.avisarPagoRecibido(pago);

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("pepe@test.com")));
        assertThat(enviado[1], is(equalTo("Has recibido un pago en Acompáñame")));
        assertThat(enviado[2], containsString("Hola Pepe"));
    }

    @Test
    void testElTextoDiceQuienPagaYElImporteConComa() {
        avisos.avisarPagoRecibido(pago);

        String texto = correoEnviado()[2];
        assertThat(texto, containsString("Ana ha pagado el servicio"));
        assertThat(texto, containsString("58,00 €"));
    }

    @Test
    void testLlevaElEnlaceAlHistorial() {
        avisos.avisarPagoRecibido(pago);

        assertThat(correoEnviado()[2], containsString(URL_FRONT + "/historial"));
    }

    @Test
    void testNoIncluyeDatosDelPacienteNiLaDireccion() {
        avisos.avisarPagoRecibido(pago);

        String texto = correoEnviado()[2];
        assertThat(texto, not(containsString("Manuel")));
        assertThat(texto, not(containsString("Calle Secreta")));
    }

    @Test
    void testSiFallaElCorreoNoSeLanzaNingunError() {
        doThrow(new CorreoException("fallo", new RuntimeException()))
                .when(correo).enviar(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> avisos.avisarPagoRecibido(pago));
    }
}
