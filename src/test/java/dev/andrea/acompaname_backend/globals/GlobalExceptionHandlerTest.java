package dev.andrea.acompaname_backend.globals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionConflicto;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionNotFound;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testUsuarioNotFound() {
        ResponseEntity<String> response = handler.handleUsuarioNotFoundException(new UsuarioExceptionNotFound("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(404));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testPerfilCuidadorNotFound() {
        ResponseEntity<String> response = handler.handlePerfilCuidadorNotFoundException(new PerfilCuidadorExceptionNotFound("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(404));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testSolicitudNotFound() {
        ResponseEntity<String> response = handler.handlerSolicitudNotFoundException(new SolicitudExceptionNotFound("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(404));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testValoracionNotFound() {
        ResponseEntity<String> response = handler.handleValoracionNotFoundException(new ValoracionExceptionNotFound("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(404));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testPagoNotFound() {
        ResponseEntity<String> response = handler.handlePagoNotFoundException(new PagoExceptionNotFound("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(404));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testUsuarioEmailDuplicado() {
        ResponseEntity<String> response = handler.handleUsuarioEmailDuplicadoException(new UsuarioExceptionEmailDuplicado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(409));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testValoracionConflicto() {
        ResponseEntity<String> response = handler.handleValoracionConflictoException(new ValoracionExceptionConflicto("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(409));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testSolicitudAccesoDenegado() {
        ResponseEntity<String> response = handler.handleSolicitudAccesoDenegadoException(new SolicitudExceptionAccesoDenegado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(403));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testPerfilCuidadorAccesoDenegado() {
        ResponseEntity<String> response = handler.handlePerfilCuidadorAccesoDenegadoException(new PerfilCuidadorExceptionAccesoDenegado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(403));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testUsuarioAccesoDenegado() {
        ResponseEntity<String> response = handler.handleUsuarioAccesoDenegadoException(new UsuarioExceptionAccesoDenegado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(403));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testValoracionAccesoDenegado() {
        ResponseEntity<String> response = handler.handleValoracionAccesoDenegadoException(new ValoracionExceptionAccesoDenegado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(403));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testPagoAccesoDenegado() {
        ResponseEntity<String> response = handler.handlePagoAccesoDenegadoException(new PagoExceptionAccesoDenegado("Mensaje de prueba"));

        assertThat(response.getStatusCode().value(), is(403));
        assertThat(response.getBody(), is(equalTo("Mensaje de prueba")));
    }

    @Test
    void testErrorNoControlado() {
        ResponseEntity<String> response = handler.handlerGenericException(new RuntimeException("Fallo inesperado"));

        assertThat(response.getStatusCode().value(), is(500));
        assertThat(response.getBody(), is(equalTo("Error interno del servidor")));
    }
}