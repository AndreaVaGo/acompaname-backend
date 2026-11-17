package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.auth.LoginDTORequest;
import dev.andrea.acompaname_backend.user.dtos.CambiarPasswordDTO;
import dev.andrea.acompaname_backend.user.dtos.UsuarioDTORequest;

// Comprueba que ninguna contraseña aparece si alguien escribe el objeto en un log
public class PasswordNoSeMuestraTest {

    @Test
    void testLoginNoMuestraLaPassword() {
        LoginDTORequest login = new LoginDTORequest("ana@test.com", "Secreta-123");

        String texto = login.toString();

        assertThat(texto, not(containsString("Secreta-123")));
        assertThat(texto, containsString("ana@test.com"));
    }

    @Test
    void testCambiarPasswordNoMuestraNingunaPassword() {
        CambiarPasswordDTO cambio = new CambiarPasswordDTO("Vieja-123!", "Nueva-456!");

        String texto = cambio.toString();

        assertThat(texto, not(containsString("Vieja-123!")));
        assertThat(texto, not(containsString("Nueva-456!")));
    }

    @Test
    void testRegistroNoMuestraLaPasswordPeroSiLosDemasDatos() {
        UsuarioDTORequest registro = new UsuarioDTORequest("Ana", "ana@test.com", "600111222",
                "Secreta-123", Set.of(1L));

        String texto = registro.toString();

        assertThat(texto, not(containsString("Secreta-123")));
        assertThat(texto, containsString("Ana"));
        assertThat(texto, containsString("ana@test.com"));
    }
}
