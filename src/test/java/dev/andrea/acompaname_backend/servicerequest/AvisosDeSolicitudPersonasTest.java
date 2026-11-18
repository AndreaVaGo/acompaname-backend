package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.mail.ServicioDeCorreo;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests de cómo aparece el número de personas a cuidar en los correos de aviso
@ExtendWith(MockitoExtension.class)
public class AvisosDeSolicitudPersonasTest {

    @Mock
    private ServicioDeCorreo correo;

    private AvisosDeSolicitud avisos;
    private SolicitudEntity solicitud;

    @BeforeEach
    void setup() {
        avisos = new AvisosDeSolicitud(correo, "http://localhost:5173");

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

    private String textoDelCorreo() {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                texto.capture());
        return texto.getValue();
    }

    @Test
    void testLaSolicitudNuevaConTresPersonasDiceCuantas() {
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Ada", 9));
        solicitud.getPersonas().add(new PersonaCuidada("Leo", 2));

        avisos.avisarSolicitudNueva(solicitud);

        assertThat(textoDelCorreo(), containsString("de 3 personas"));
    }

    @Test
    void testLaSolicitudNuevaConUnaPersonaNoDiceElNumero() {
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));

        avisos.avisarSolicitudNueva(solicitud);

        assertThat(textoDelCorreo(), not(containsString("personas")));
    }

    @Test
    void testElCorreoNoMuestraLosNombresDeLasPersonas() {
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Ada", 9));

        avisos.avisarSolicitudNueva(solicitud);

        String texto = textoDelCorreo();
        assertThat(texto, not(containsString("Zoe")));
        assertThat(texto, not(containsString("Ada")));
    }

    @Test
    void testLaSolicitudAceptadaConVariasPersonasDiceCuantas() {
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        avisos.avisarSolicitudAceptada(solicitud);

        assertThat(textoDelCorreo(), containsString("de 2 personas"));
    }

    @Test
    void testLaSolicitudRechazadaConVariasPersonasDiceCuantas() {
        solicitud.getPersonas().add(new PersonaCuidada("Carmen", 82));
        solicitud.getPersonas().add(new PersonaCuidada("Antonio", 85));

        avisos.avisarSolicitudRechazada(solicitud);

        assertThat(textoDelCorreo(), containsString("de 2 personas"));
    }
}
