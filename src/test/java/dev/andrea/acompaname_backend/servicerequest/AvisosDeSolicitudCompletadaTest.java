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
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Comprueba el correo que recibe la familia cuando el servicio se completa
@ExtendWith(MockitoExtension.class)
public class AvisosDeSolicitudCompletadaTest {

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
                .nombrePaciente("Manuel")
                .direccion("Calle Secreta 5")
                .fechaCuidado(LocalDate.of(2026, 12, 15))
                .estado(EstadoSolicitud.COMPLETADA)
                .familia(ana)
                .cuidador(perfilPepe)
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
    void testElAvisoVaALaFamilia() {
        avisos.avisarServicioCompletado(solicitud, new BigDecimal("58.00"));

        String[] enviado = correoEnviado();
        assertThat(enviado[0], is(equalTo("ana@test.com")));
        assertThat(enviado[1], is(equalTo("Tu servicio ha sido completado")));
        assertThat(enviado[2], containsString("Hola Ana"));
        assertThat(enviado[2], containsString("Pepe ha completado el servicio de cuidado (Acompañamiento)"));
    }

    @Test
    void testDiceElImportePendienteConComa() {
        avisos.avisarServicioCompletado(solicitud, new BigDecimal("58"));

        assertThat(correoEnviado()[2], containsString("pago pendiente de 58,00 €"));
    }

    @Test
    void testLlevaElEnlaceALosPagos() {
        avisos.avisarServicioCompletado(solicitud, new BigDecimal("58.00"));

        assertThat(correoEnviado()[2], containsString(URL_FRONT + "/pagos"));
    }

    @Test
    void testNoIncluyeElNombreDelPacienteNiLaDireccion() {
        avisos.avisarServicioCompletado(solicitud, new BigDecimal("58.00"));

        String texto = correoEnviado()[2];
        assertThat(texto, not(containsString("Manuel")));
        assertThat(texto, not(containsString("Calle Secreta")));
    }

    @Test
    void testSiFallaElCorreoNoSeLanzaNingunError() {
        doThrow(new CorreoException("fallo", new RuntimeException()))
                .when(correo).enviar(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> avisos.avisarServicioCompletado(solicitud, new BigDecimal("58.00")));
    }
}
