package dev.andrea.acompaname_backend.globals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.payment.exceptions.PagoException;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorException;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudException;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioException;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionException;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionNotFound;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionConflicto;

public class ExceptionsConCausaTest {

    private final Throwable causa = new RuntimeException("Causa original");

    private void comprobar(Throwable excepcion) {
        assertThat(excepcion.getMessage(), is(equalTo("Mensaje")));
        assertThat(excepcion.getCause(), is(sameInstance(causa)));
    }

    @Test
    void testExcepcionesPagoConCausa() {
        comprobar(new PagoException("Mensaje", causa));
        comprobar(new PagoExceptionNotFound("Mensaje", causa));
        comprobar(new PagoExceptionAccesoDenegado("Mensaje", causa));
    }

    @Test
    void testExcepcionesPerfilCuidadorConCausa() {
        comprobar(new PerfilCuidadorException("Mensaje", causa));
        comprobar(new PerfilCuidadorExceptionNotFound("Mensaje", causa));
        comprobar(new PerfilCuidadorExceptionAccesoDenegado("Mensaje", causa));
    }

    @Test
    void testExcepcionesSolicitudConCausa() {
        comprobar(new SolicitudException("Mensaje", causa));
        comprobar(new SolicitudExceptionNotFound("Mensaje", causa));
        comprobar(new SolicitudExceptionAccesoDenegado("Mensaje", causa));
    }

    @Test
    void testExcepcionesUsuarioConCausa() {
        comprobar(new UsuarioException("Mensaje", causa));
        comprobar(new UsuarioExceptionNotFound("Mensaje", causa));
        comprobar(new UsuarioExceptionAccesoDenegado("Mensaje", causa));
        comprobar(new UsuarioExceptionEmailDuplicado("Mensaje", causa));
    }

    @Test
    void testExcepcionesValoracionConCausa() {
        comprobar(new ValoracionException("Mensaje", causa));
        comprobar(new ValoracionExceptionNotFound("Mensaje", causa));
        comprobar(new ValoracionExceptionAccesoDenegado("Mensaje", causa));
        comprobar(new ValoracionExceptionConflicto("Mensaje", causa));
    }
}